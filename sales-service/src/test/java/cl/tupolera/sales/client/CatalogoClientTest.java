package cl.tupolera.sales.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import cl.tupolera.sales.client.InventarioClienteException.Tipo;
import cl.tupolera.sales.client.dto.ConsultaCatalogo;
import cl.tupolera.sales.client.dto.ItemInventario;
import cl.tupolera.sales.client.dto.ResultadoInventario;
import cl.tupolera.sales.exception.ApiException;

/** Prueba el cliente contra un servidor simulado: no abre red ni necesita catalog-service. */
class CatalogoClientTest {

    private static final String BASE = "http://catalogo.test";
    private static final String CLAVE = "clave-interna-de-prueba-0123456789abcdef";
    private static final String REF = "TP-3f2b8c9e-6a41-4d52-9c1e-0a7b5d2e8f10";

    private MockRestServiceServer server;
    private CatalogoClient client;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CatalogoClient(builder.build(),
                new ClientesProperties(BASE, "http://auth.test", CLAVE, null, null));
    }

    private static List<ItemInventario> items() {
        return List.of(new ItemInventario(1L, "M", 2));
    }

    private void respondeDescuento(org.springframework.test.web.client.ResponseCreator respuesta) {
        server.expect(requestTo(BASE + "/internal/inventario/descuento"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(respuesta);
    }

    private static org.springframework.test.web.client.ResponseCreator json(HttpStatus status, String cuerpo) {
        return withStatus(status).contentType(MediaType.APPLICATION_JSON).body(cuerpo);
    }

    /* ----------------------------- consulta ----------------------------- */

    @Test
    void consultaEnviaLaClaveInternaYMapeaLaRespuesta() {
        server.expect(requestTo(BASE + "/internal/inventario/consulta"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Key", CLAVE))
                .andExpect(jsonPath("$.productoIds[0]").value(1))
                .andExpect(jsonPath("$.productoIds[1]").value(9))
                .andRespond(withSuccess("""
                        {"productos":[{"id":1,"codigo":"P001","nombre":"Polera","activo":true,"precio":10000,
                          "descuento":10,"precioFinal":9000,"categoriaId":3,
                          "variantes":[{"id":11,"talla":"M","stock":5}],"campoNuevo":"se ignora"}],
                         "noEncontrados":[9]}""", MediaType.APPLICATION_JSON));

        ConsultaCatalogo r = client.consultar(List.of(1L, 9L));

        assertThat(r.productos()).hasSize(1);
        assertThat(r.productos().get(0).precioFinal()).isEqualTo(9000);
        assertThat(r.productos().get(0).variantes().get(0).stock()).isEqualTo(5);
        assertThat(r.noEncontrados()).containsExactly(9L);
        server.verify();
    }

    @Test
    void consultaConClaveRechazadaDevuelve503SinFiltrarDetalles() {
        server.expect(requestTo(BASE + "/internal/inventario/consulta")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.consultar(List.of(1L)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getMessage()).doesNotContain(CLAVE).doesNotContain("401");
                });
    }

    @Test
    void consultaSinConexionDevuelve503() {
        server.expect(requestTo(BASE + "/internal/inventario/consulta"))
                .andRespond(withException(new ConnectException("rechazada")));

        assertThatThrownBy(() -> client.consultar(List.of(1L)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void sinClaveInternaConfiguradaNoLlamaAlCatalogo() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer vacio = MockRestServiceServer.bindTo(builder).build();
        CatalogoClient sinClave = new CatalogoClient(builder.build(),
                new ClientesProperties(BASE, "http://auth.test", "", null, null));

        assertThatThrownBy(() -> sinClave.consultar(List.of(1L))).isInstanceOf(ApiException.class);
        vacio.verify(); // no se esperaba ninguna llamada
    }

    /* ----------------------------- descuento ----------------------------- */

    @Test
    void descuentoExitosoEnviaReferenciaEItems() {
        server.expect(requestTo(BASE + "/internal/inventario/descuento"))
                .andExpect(header("X-Internal-Key", CLAVE))
                .andExpect(jsonPath("$.referencia").value(REF))
                .andExpect(jsonPath("$.items[0].productoId").value(1))
                .andExpect(jsonPath("$.items[0].talla").value("M"))
                .andExpect(jsonPath("$.items[0].cantidad").value(2))
                .andRespond(withSuccess("""
                        {"referencia":"%s","tipo":"DESCUENTO","repetida":false,
                         "items":[{"productoId":1,"varianteId":11,"talla":"M","cantidad":2,"stockActual":3}]}
                        """.formatted(REF), MediaType.APPLICATION_JSON));

        ResultadoInventario r = client.descontar(REF, items());

        assertThat(r.repetida()).isFalse();
        assertThat(r.items().get(0).stockActual()).isEqualTo(3);
        server.verify();
    }

    @Test
    void descuentoRepetidoSeInformaComoRepetida() {
        respondeDescuento(withSuccess("""
                {"referencia":"%s","tipo":"DESCUENTO","repetida":true,"items":[]}""".formatted(REF),
                MediaType.APPLICATION_JSON));

        assertThat(client.descontar(REF, items()).repetida()).isTrue();
    }

    @Test
    void stockInsuficienteEsRechazoDefinitivoConDetalle() {
        respondeDescuento(json(HttpStatus.CONFLICT, """
                {"status":409,"message":"No hay stock suficiente para completar el descuento.",
                 "errores":[{"productoId":1,"talla":"M","codigo":"STOCK_INSUFICIENTE","mensaje":"x","disponible":1}]}"""));

        assertThatThrownBy(() -> client.descontar(REF, items()))
                .isInstanceOfSatisfying(InventarioClienteException.class, e -> {
                    assertThat(e.getTipo()).isEqualTo(Tipo.RECHAZADO);
                    assertThat(e.getStatus()).isEqualTo(409);
                    assertThat(e.tieneCodigo("STOCK_INSUFICIENTE")).isTrue();
                    assertThat(e.getErrores().get(0).disponible()).isEqualTo(1);
                });
    }

    @Test
    void error400y404SonRechazosDefinitivos() {
        respondeDescuento(json(HttpStatus.BAD_REQUEST, "{\"message\":\"referencia inválida\"}"));
        assertThatThrownBy(() -> client.descontar(REF, items()))
                .isInstanceOfSatisfying(InventarioClienteException.class,
                        e -> assertThat(e.getTipo()).isEqualTo(Tipo.RECHAZADO));

        server.reset(); // MockRestServiceServer no admite nuevas expectativas tras una petición
        respondeDescuento(json(HttpStatus.NOT_FOUND,
                "{\"errores\":[{\"codigo\":\"PRODUCTO_NO_ENCONTRADO\",\"productoId\":1}]}"));
        assertThatThrownBy(() -> client.descontar(REF, items()))
                .isInstanceOfSatisfying(InventarioClienteException.class, e -> {
                    assertThat(e.getTipo()).isEqualTo(Tipo.RECHAZADO);
                    assertThat(e.tieneCodigo("PRODUCTO_NO_ENCONTRADO")).isTrue();
                });
    }

    @Test
    void error503SignificaInventarioOcupadoYNoAplicado() {
        respondeDescuento(json(HttpStatus.SERVICE_UNAVAILABLE, "{\"message\":\"ocupado\"}"));

        assertThatThrownBy(() -> client.descontar(REF, items()))
                .isInstanceOfSatisfying(InventarioClienteException.class,
                        e -> assertThat(e.getTipo()).isEqualTo(Tipo.REINTENTABLE));
    }

    @Test
    void claveInternaRechazadaEsErrorDeConfiguracion() {
        respondeDescuento(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.descontar(REF, items()))
                .isInstanceOfSatisfying(InventarioClienteException.class, e -> {
                    assertThat(e.getTipo()).isEqualTo(Tipo.CONFIGURACION);
                    assertThat(e.getMessage()).doesNotContain(CLAVE);
                });
    }

    @Test
    void error500NoSeTrataComoFalloSinoComoIncierto() {
        respondeDescuento(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.descontar(REF, items()))
                .isInstanceOfSatisfying(InventarioClienteException.class,
                        e -> assertThat(e.getTipo()).isEqualTo(Tipo.INCIERTO));
    }

    @Test
    void tiempoDeEsperaAgotadoEsIncierto() {
        respondeDescuento(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.descontar(REF, items()))
                .isInstanceOfSatisfying(InventarioClienteException.class,
                        e -> assertThat(e.getTipo()).isEqualTo(Tipo.INCIERTO));
    }

    @Test
    void noPoderConectarSeConsideraNoAplicadoYReintentable() {
        respondeDescuento(withException(new ConnectException("Connection refused")));

        assertThatThrownBy(() -> client.descontar(REF, items()))
                .isInstanceOfSatisfying(InventarioClienteException.class,
                        e -> assertThat(e.getTipo()).isEqualTo(Tipo.REINTENTABLE));
    }

    @Test
    void respuestaExitosaIlegibleEsIncierta() {
        respondeDescuento(withSuccess("esto no es json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.descontar(REF, items()))
                .isInstanceOfSatisfying(InventarioClienteException.class,
                        e -> assertThat(e.getTipo()).isEqualTo(Tipo.INCIERTO));
    }

    @Test
    void respuestaExitosaSinCuerpoEsIncierta() {
        respondeDescuento(withStatus(HttpStatus.OK));

        assertThatThrownBy(() -> client.descontar(REF, items()))
                .isInstanceOfSatisfying(InventarioClienteException.class,
                        e -> assertThat(e.getTipo()).isEqualTo(Tipo.INCIERTO));
    }

    /* ----------------------------- reversión ----------------------------- */

    @Test
    void reversionUsaLaReferenciaEnLaRuta() {
        server.expect(requestTo(BASE + "/internal/inventario/descuento/" + REF + "/reversion"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Key", CLAVE))
                .andExpect(content().string(""))
                .andRespond(withSuccess("""
                        {"referencia":"%s","tipo":"REVERSION","repetida":false,"items":[]}""".formatted(REF),
                        MediaType.APPLICATION_JSON));

        assertThat(client.revertir(REF).tipo()).isEqualTo("REVERSION");
        server.verify();
    }

    @Test
    void reversionDeUnaReferenciaNuncaDescontadaSeDistingueDelResto() {
        server.expect(requestTo(BASE + "/internal/inventario/descuento/" + REF + "/reversion"))
                .andRespond(json(HttpStatus.NOT_FOUND,
                        "{\"errores\":[{\"codigo\":\"DESCUENTO_NO_ENCONTRADO\"}]}"));

        assertThatThrownBy(() -> client.revertir(REF))
                .isInstanceOfSatisfying(InventarioClienteException.class, e -> {
                    assertThat(e.getTipo()).isEqualTo(Tipo.RECHAZADO);
                    assertThat(e.tieneCodigo("DESCUENTO_NO_ENCONTRADO")).isTrue();
                });
    }

    /* ----------------------------- imagen ----------------------------- */

    @Test
    void imagenPequenaSeObtieneDelDetalleDelProducto() {
        server.expect(requestTo(BASE + "/api/v1/productos/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1,\"nombre\":\"x\",\"imagen\":\"polera.png\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.obtenerImagen(1L)).contains("polera.png");
    }

    @Test
    void imagenMayorAUnMegaNoSeGuarda() {
        String grande = "a".repeat(PoliticaImagen.MAXIMO_BYTES + 1);
        server.expect(requestTo(BASE + "/api/v1/productos/1"))
                .andRespond(withSuccess("{\"id\":1,\"imagen\":\"" + grande + "\"}", MediaType.APPLICATION_JSON));

        assertThat(client.obtenerImagen(1L)).isEmpty();
    }

    @Test
    void siFallaLaImagenLaCompraNoSeInterrumpe() {
        server.expect(requestTo(BASE + "/api/v1/productos/1")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(client.obtenerImagen(1L)).isEmpty();

        server.reset(); // MockRestServiceServer no admite nuevas expectativas tras una petición
        server.expect(requestTo(BASE + "/api/v1/productos/2")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThat(client.obtenerImagen(2L)).isEmpty();
    }
}
