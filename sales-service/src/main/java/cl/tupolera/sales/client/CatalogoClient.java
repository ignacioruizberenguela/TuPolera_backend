package cl.tupolera.sales.client;

import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import cl.tupolera.sales.client.InventarioClienteException.Tipo;
import cl.tupolera.sales.client.dto.ConsultaCatalogo;
import cl.tupolera.sales.client.dto.ErrorCatalogo;
import cl.tupolera.sales.client.dto.ErrorItemInventario;
import cl.tupolera.sales.client.dto.ItemInventario;
import cl.tupolera.sales.client.dto.ProductoImagen;
import cl.tupolera.sales.client.dto.ResultadoInventario;
import cl.tupolera.sales.exception.ApiException;

/**
 * Cliente de catalog-service. Usa solo contratos que ya existen:
 *  - POST /internal/inventario/consulta, /descuento, /descuento/{referencia}/reversion (cabecera X-Internal-Key)
 *  - GET /api/v1/productos/{id} (público) para la imagen de la boleta.
 */
@Component
public class CatalogoClient {

    static final String CABECERA_CLAVE = "X-Internal-Key";

    private static final Logger log = LoggerFactory.getLogger(CatalogoClient.class);

    private final RestClient rest;
    private final ClientesProperties props;

    public CatalogoClient(@Qualifier("catalogoRestClient") RestClient rest, ClientesProperties props) {
        this.rest = rest;
        this.props = props;
    }

    /* ---------------------------------------------------------------- */
    /* Consulta de productos, precios y stock                            */
    /* ---------------------------------------------------------------- */

    /** Precios reales y stock. Si catalog-service no responde bien, la compra no puede continuar: 503. */
    public ConsultaCatalogo consultar(List<Long> productoIds) {
        try {
            ConsultaCatalogo respuesta = rest.post()
                    .uri("/internal/inventario/consulta")
                    .header(CABECERA_CLAVE, claveInterna())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("productoIds", productoIds))
                    .retrieve()
                    .body(ConsultaCatalogo.class);
            if (respuesta == null) {
                throw noDisponible("catalog-service devolvió una respuesta vacía en la consulta.", null);
            }
            return respuesta;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401) {
                log.error("catalog-service rechazó la clave interna (401). Revisa INTERNAL_API_KEY.");
            }
            throw noDisponible("catalog-service respondió " + ex.getStatusCode().value() + " en la consulta.", ex);
        } catch (RestClientException ex) {
            throw noDisponible("No se pudo consultar catalog-service: " + ex.getClass().getSimpleName(), ex);
        }
    }

    /* ---------------------------------------------------------------- */
    /* Descuento y reversión                                              */
    /* ---------------------------------------------------------------- */

    /** Descuenta stock de forma atómica e idempotente por "referencia". Ver InventarioClienteException. */
    public ResultadoInventario descontar(String referencia, List<ItemInventario> items) {
        return ejecutar("descuento", () -> rest.post()
                .uri("/internal/inventario/descuento")
                .header(CABECERA_CLAVE, claveInterna())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("referencia", referencia, "items", items))
                .retrieve()
                .body(ResultadoInventario.class));
    }

    /**
     * Devuelve lo descontado por esa referencia. Es idempotente: repetirla devuelve "repetida=true".
     * Un 404 con DESCUENTO_NO_ENCONTRADO (RECHAZADO) significa que esa referencia nunca se descontó.
     */
    public ResultadoInventario revertir(String referencia) {
        return ejecutar("reversión", () -> rest.post()
                .uri("/internal/inventario/descuento/{referencia}/reversion", referencia)
                .header(CABECERA_CLAVE, claveInterna())
                .retrieve()
                .body(ResultadoInventario.class));
    }

    private ResultadoInventario ejecutar(String operacion, Supplier<ResultadoInventario> llamada) {
        try {
            ResultadoInventario resultado = llamada.get();
            if (resultado == null) {
                // Respondió 2xx sin cuerpo: probablemente se aplicó, pero no se puede confirmar.
                throw new InventarioClienteException(Tipo.INCIERTO, 200,
                        "catalog-service respondió sin contenido en el " + operacion + ".", List.of(), null);
            }
            return resultado;
        } catch (InventarioClienteException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw clasificarRespuesta(operacion, ex);
        } catch (ResourceAccessException ex) {
            if (noLlegoAlServidor(ex)) {
                throw new InventarioClienteException(Tipo.REINTENTABLE, 0,
                        "No se pudo conectar con catalog-service (" + operacion + ").", List.of(), ex);
            }
            throw new InventarioClienteException(Tipo.INCIERTO, 0,
                    "Sin respuesta confirmada de catalog-service en el " + operacion + ".", List.of(), ex);
        } catch (RestClientException ex) {
            // Por ejemplo, un 200 con cuerpo ilegible: el servidor pudo haber aplicado la operación.
            throw new InventarioClienteException(Tipo.INCIERTO, 0,
                    "Respuesta ilegible de catalog-service en el " + operacion + ".", List.of(), ex);
        }
    }

    private InventarioClienteException clasificarRespuesta(String operacion, RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        List<ErrorItemInventario> errores = leerErrores(ex);

        return switch (status) {
            case 400, 404, 409 -> new InventarioClienteException(Tipo.RECHAZADO, status,
                    "catalog-service rechazó el " + operacion + " (" + status + ").", errores, ex);
            case 503 -> new InventarioClienteException(Tipo.REINTENTABLE, status,
                    "El inventario está ocupado; no se aplicó el " + operacion + ".", errores, ex);
            case 401 -> {
                log.error("catalog-service rechazó la clave interna (401). Revisa INTERNAL_API_KEY.");
                yield new InventarioClienteException(Tipo.CONFIGURACION, status,
                        "La clave interna con catalog-service no es válida.", errores, ex);
            }
            // Cualquier otro código: no se puede afirmar que no se aplicó.
            default -> new InventarioClienteException(Tipo.INCIERTO, status,
                    "Respuesta inesperada de catalog-service (" + status + ") en el " + operacion + ".", errores, ex);
        };
    }

    private static List<ErrorItemInventario> leerErrores(RestClientResponseException ex) {
        try {
            ErrorCatalogo cuerpo = ex.getResponseBodyAs(ErrorCatalogo.class);
            return cuerpo == null || cuerpo.errores() == null ? List.of() : cuerpo.errores();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /** true solo si el fallo ocurrió al conectar, antes de enviar la solicitud. */
    static boolean noLlegoAlServidor(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof HttpConnectTimeoutException || t instanceof ConnectException) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    /* ---------------------------------------------------------------- */
    /* Imagen para la boleta                                              */
    /* ---------------------------------------------------------------- */

    /**
     * Imagen del producto para el snapshot de la boleta. Es opcional: si no hay imagen, pesa más de 1 MB o el
     * catálogo no responde, la boleta se guarda sin imagen y la compra continúa.
     */
    public Optional<String> obtenerImagen(Long productoId) {
        try {
            ProductoImagen producto = rest.get()
                    .uri("/api/v1/productos/{id}", productoId)
                    .retrieve()
                    .body(ProductoImagen.class);
            return producto == null ? Optional.empty() : PoliticaImagen.aplicar(producto.imagen());
        } catch (RestClientException ex) {
            log.warn("No se pudo obtener la imagen del producto {}: {}", productoId, ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /* ---------------------------------------------------------------- */

    private String claveInterna() {
        String clave = props.internalApiKey();
        if (clave == null || clave.isBlank()) {
            log.error("INTERNAL_API_KEY no está configurada en sales-service.");
            throw noDisponible("La comunicación interna con catalog-service no está configurada.", null);
        }
        return clave;
    }

    private static ApiException noDisponible(String detalleInterno, Throwable causa) {
        // El detalle técnico queda solo en el log; el cliente recibe un mensaje genérico.
        log.warn("{}", detalleInterno, causa);
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "El catálogo no está disponible por ahora. Inténtalo nuevamente en unos segundos.");
    }
}
