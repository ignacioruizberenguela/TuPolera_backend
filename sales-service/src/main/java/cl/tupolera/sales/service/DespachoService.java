package cl.tupolera.sales.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.tupolera.sales.dto.ConfiguracionDespachoRequest;
import cl.tupolera.sales.dto.ConfiguracionDespachoResponse;
import cl.tupolera.sales.dto.CotizacionResponse;
import cl.tupolera.sales.dto.DestinoRequest;
import cl.tupolera.sales.dto.OrigenResponse;
import cl.tupolera.sales.exception.ApiException;
import cl.tupolera.sales.model.ConfiguracionDespacho;
import cl.tupolera.sales.repository.ConfiguracionDespachoRepository;
import cl.tupolera.sales.util.Geo;
import cl.tupolera.sales.util.PrecioDespacho;

/**
 * Configuración y cotización del despacho.
 *
 * El frontend (Google Maps) envía la dirección y las coordenadas del destino; el backend las valida, calcula
 * SIEMPRE la distancia (Haversine, línea recta) y el precio con la configuración guardada en la base de datos.
 * Nunca se confía en una distancia ni en un precio enviados por el cliente.
 */
@Service
public class DespachoService {

    private final ConfiguracionDespachoRepository repository;

    public DespachoService(ConfiguracionDespachoRepository repository) {
        this.repository = repository;
    }

    /** Configuración vigente. Si por algún motivo no existe, se crea con los valores iniciales. */
    @Transactional
    public ConfiguracionDespacho obtenerConfiguracion() {
        return repository.findById(ConfiguracionDespacho.ID_UNICO)
                .orElseGet(() -> repository.save(ConfiguracionDespacho.porDefecto()));
    }

    @Transactional
    public ConfiguracionDespachoResponse consultarConfiguracion() {
        return aRespuesta(obtenerConfiguracion());
    }

    /**
     * @param idAdministrador id del administrador autenticado (viene del JWT); queda registrado como autor.
     */
    @Transactional
    public ConfiguracionDespachoResponse actualizarConfiguracion(ConfiguracionDespachoRequest request,
            Long idAdministrador) {

        double latitud = request.latitudBodega();
        double longitud = request.longitudBodega();

        if (!Geo.estaEnChile(latitud, longitud)) {
            throw ApiException.badRequest("La ubicación de la bodega debe estar dentro de Chile.");
        }

        Long minima = request.tarifaMinima();
        Long maxima = request.tarifaMaxima();

        if (minima != null && maxima != null && maxima < minima) {
            throw ApiException.badRequest("La tarifa máxima no puede ser menor que la tarifa mínima.");
        }

        String nombre = limpiar(request.nombreBodega());
        String direccion = limpiar(request.direccionBodega());

        if (nombre.isEmpty() || direccion.isEmpty()) {
            throw ApiException.badRequest("El nombre y la dirección de la bodega no pueden estar vacíos.");
        }

        ConfiguracionDespacho configuracion = obtenerConfiguracion();
        configuracion.setNombreBodega(nombre);
        configuracion.setDireccionBodega(direccion);
        configuracion.setLatitudBodega(latitud);
        configuracion.setLongitudBodega(longitud);
        configuracion.setPrecioBase(request.precioBase());
        configuracion.setCostoPorKm(request.costoPorKm());
        configuracion.setTarifaMinima(minima);
        configuracion.setTarifaMaxima(maxima);
        configuracion.setEsUbicacionPrueba(request.esUbicacionPrueba());
        configuracion.setFechaActualizacion(LocalDateTime.now(ZoneOffset.UTC));
        configuracion.setActualizadoPor(idAdministrador);

        return aRespuesta(repository.save(configuracion));
    }

    /** Cotización con la configuración vigente. */
    @Transactional
    public CotizacionResponse cotizar(DestinoRequest destino) {
        return cotizar(destino, obtenerConfiguracion());
    }

    /**
     * Cotización con una configuración dada (la usará la creación de compras, que debe guardar el cálculo
     * exacto con la configuración que se usó).
     */
    public CotizacionResponse cotizar(DestinoRequest destino, ConfiguracionDespacho configuracion) {
        String direccion = limpiar(destino.direccion());

        if (direccion.isEmpty()) {
            throw ApiException.badRequest("La dirección de despacho es obligatoria.");
        }

        if (destino.latitud() == null || destino.longitud() == null) {
            throw ApiException.badRequest("La latitud y la longitud del destino son obligatorias.");
        }

        double latitud = destino.latitud();
        double longitud = destino.longitud();

        // Números válidos (ni NaN ni infinito) y rangos geográficos correctos.
        if (!Geo.esCoordenadaValida(latitud, longitud)) {
            throw ApiException.badRequest("Las coordenadas del destino no son válidas.");
        }

        // Regla del proyecto: los despachos son solo dentro de Chile.
        if (!Geo.estaEnChile(latitud, longitud)) {
            throw ApiException.badRequest("El destino debe estar dentro de Chile.");
        }

        double distanciaKm = Geo.distanciaKm(configuracion.getLatitudBodega(), configuracion.getLongitudBodega(),
                latitud, longitud);

        // El precio se calcula con la distancia exacta; en la respuesta se muestra redondeada a 3 decimales.
        long precio = PrecioDespacho.calcular(distanciaKm, configuracion.getPrecioBase(),
                configuracion.getCostoPorKm(), configuracion.getTarifaMinima(), configuracion.getTarifaMaxima());

        return new CotizacionResponse(direccion, latitud, longitud, redondear3(distanciaKm),
                CotizacionResponse.HAVERSINE, precio,
                new OrigenResponse(configuracion.getNombreBodega(), configuracion.getDireccionBodega(),
                        configuracion.getLatitudBodega(), configuracion.getLongitudBodega(),
                        configuracion.isEsUbicacionPrueba()));
    }

    private static ConfiguracionDespachoResponse aRespuesta(ConfiguracionDespacho c) {
        return new ConfiguracionDespachoResponse(c.getNombreBodega(), c.getDireccionBodega(), c.getLatitudBodega(),
                c.getLongitudBodega(), c.getPrecioBase(), c.getCostoPorKm(), c.getTarifaMinima(),
                c.getTarifaMaxima(), c.isEsUbicacionPrueba(), c.getFechaActualizacion().toInstant(ZoneOffset.UTC));
    }

    /** Quita espacios de los extremos y colapsa los espacios repetidos. */
    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
    }

    private static double redondear3(double valor) {
        return Math.round(valor * 1000) / 1000.0;
    }
}
