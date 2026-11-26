package cl.tupolera.sales.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import cl.tupolera.sales.config.SecurityConfig;
import cl.tupolera.sales.dto.ConfiguracionDespachoResponse;
import cl.tupolera.sales.dto.CotizacionResponse;
import cl.tupolera.sales.dto.OrigenResponse;
import cl.tupolera.sales.security.ApiSecurityHandlers;
import cl.tupolera.sales.service.DespachoService;

/** Autenticación, roles y validaciones de los endpoints de despacho (capa web, sin base de datos). */
@WebMvcTest(controllers = DespachoController.class)
@Import({ SecurityConfig.class, ApiSecurityHandlers.class })
class DespachoWebTest {

    private static final String CONFIGURACION_VALIDA = """
            {"nombreBodega":"Bodega Central","direccionBodega":"Av. Siempre Viva 742, Santiago",
             "latitudBodega":-33.45,"longitudBodega":-70.66,"precioBase":3500,"costoPorKm":200,
             "tarifaMinima":3000,"tarifaMaxima":18000,"esUbicacionPrueba":false}
            """;

    private static final String DESTINO_VALIDO = """
            {"direccion":"Av. Providencia 1234, Providencia, Santiago","latitud":-33.4251,"longitud":-70.6167}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private DespachoService despachoService;
    @MockitoBean private JwtDecoder jwtDecoder;

    private static RequestPostProcessor como(String rol) {
        return jwt().jwt(token -> token.subject("7")).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private static ConfiguracionDespachoResponse configuracion() {
        return new ConfiguracionDespachoResponse("Bodega", "Calle 1", -33.4372, -70.6506, 2990, 150, 2990L, 15000L,
                true, Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static CotizacionResponse cotizacion() {
        return new CotizacionResponse("Av. Providencia 1234", -33.4251, -70.6167, 3.421,
                CotizacionResponse.HAVERSINE, 3500,
                new OrigenResponse("Bodega", "Calle 1", -33.4372, -70.6506, true));
    }

    /* ----------------------------- autenticación ----------------------------- */

    @Test
    void sinTokenTodosLosEndpointsResponden401() throws Exception {
        mockMvc.perform(get("/api/v1/despacho/configuracion"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(put("/api/v1/despacho/configuracion").contentType(MediaType.APPLICATION_JSON)
                .content(CONFIGURACION_VALIDA)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/despacho/cotizacion").contentType(MediaType.APPLICATION_JSON)
                .content(DESTINO_VALIDO)).andExpect(status().isUnauthorized());

        verifyNoInteractions(despachoService);
    }

    @Test
    void cualquierUsuarioAutenticadoPuedeConsultarLaConfiguracionYCotizar() throws Exception {
        when(despachoService.consultarConfiguracion()).thenReturn(configuracion());
        when(despachoService.cotizar(any())).thenReturn(cotizacion());

        for (String rol : new String[] { "CLIENTE", "VENDEDOR", "ADMINISTRADOR" }) {
            mockMvc.perform(get("/api/v1/despacho/configuracion").with(como(rol)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.precioBase").value(2990));

            mockMvc.perform(post("/api/v1/despacho/cotizacion").with(como(rol))
                    .contentType(MediaType.APPLICATION_JSON).content(DESTINO_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.precio").value(3500))
                    .andExpect(jsonPath("$.tipoCalculo").value("HAVERSINE_LINEA_RECTA"));
        }
    }

    /* ----------------------------- modificar la configuración ----------------------------- */

    @Test
    void soloElAdministradorPuedeModificarLaConfiguracion() throws Exception {
        for (String rol : new String[] { "CLIENTE", "VENDEDOR" }) {
            mockMvc.perform(put("/api/v1/despacho/configuracion").with(como(rol))
                    .contentType(MediaType.APPLICATION_JSON).content(CONFIGURACION_VALIDA))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        verifyNoInteractions(despachoService);
    }

    @Test
    void elAdministradorModificaLaConfiguracionYQuedaRegistradoSuId() throws Exception {
        when(despachoService.actualizarConfiguracion(any(), eq(7L))).thenReturn(configuracion());

        mockMvc.perform(put("/api/v1/despacho/configuracion").with(como("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON).content(CONFIGURACION_VALIDA))
                .andExpect(status().isOk());

        verify(despachoService).actualizarConfiguracion(any(), eq(7L));   // el id sale del token (sub = 7)
    }

    @Test
    void laConfiguracionConDatosInvalidosResponde400() throws Exception {
        String[] invalidos = {
                // precio base negativo
                "{\"nombreBodega\":\"B\",\"direccionBodega\":\"D\",\"latitudBodega\":-33,\"longitudBodega\":-70,\"precioBase\":-1,\"costoPorKm\":150,\"esUbicacionPrueba\":true}",
                // costo por km faltante
                "{\"nombreBodega\":\"B\",\"direccionBodega\":\"D\",\"latitudBodega\":-33,\"longitudBodega\":-70,\"precioBase\":100,\"esUbicacionPrueba\":true}",
                // latitud fuera de rango
                "{\"nombreBodega\":\"B\",\"direccionBodega\":\"D\",\"latitudBodega\":95,\"longitudBodega\":-70,\"precioBase\":100,\"costoPorKm\":1,\"esUbicacionPrueba\":true}",
                // nombre en blanco
                "{\"nombreBodega\":\"\",\"direccionBodega\":\"D\",\"latitudBodega\":-33,\"longitudBodega\":-70,\"precioBase\":100,\"costoPorKm\":1,\"esUbicacionPrueba\":true}",
                // falta indicar si es ubicación de prueba
                "{\"nombreBodega\":\"B\",\"direccionBodega\":\"D\",\"latitudBodega\":-33,\"longitudBodega\":-70,\"precioBase\":100,\"costoPorKm\":1}"
        };

        for (String cuerpo : invalidos) {
            mockMvc.perform(put("/api/v1/despacho/configuracion").with(como("ADMINISTRADOR"))
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles").isArray());
        }

        verifyNoInteractions(despachoService);
    }

    /* ----------------------------- validación de la cotización ----------------------------- */

    private void cotizacionInvalida(String cuerpo) throws Exception {
        mockMvc.perform(post("/api/v1/despacho/cotizacion").with(como("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laCotizacionConDatosInvalidosResponde400() throws Exception {
        cotizacionInvalida("{\"direccion\":\"\",\"latitud\":-33.4,\"longitud\":-70.6}");
        cotizacionInvalida("{\"direccion\":\"   \",\"latitud\":-33.4,\"longitud\":-70.6}");
        cotizacionInvalida("{\"latitud\":-33.4,\"longitud\":-70.6}");
        cotizacionInvalida("{\"direccion\":\"Calle 1\",\"longitud\":-70.6}");
        cotizacionInvalida("{\"direccion\":\"Calle 1\",\"latitud\":-33.4}");
        cotizacionInvalida("{\"direccion\":\"Calle 1\",\"latitud\":91,\"longitud\":-70.6}");
        cotizacionInvalida("{\"direccion\":\"Calle 1\",\"latitud\":-33.4,\"longitud\":-181}");
        cotizacionInvalida("{\"direccion\":\"Calle 1\",\"latitud\":\"abc\",\"longitud\":-70.6}");
        cotizacionInvalida("{\"direccion\":\"Calle 1\",\"latitud\":NaN,\"longitud\":-70.6}");
        cotizacionInvalida("{\"direccion\":\"Calle 1\",\"latitud\":Infinity,\"longitud\":-70.6}");
        cotizacionInvalida("esto no es json");
        cotizacionInvalida("");
    }

    @Test
    void laCotizacionNoAceptaUnaDistanciaEnviadaPorElCliente() throws Exception {
        // Si el cliente envía "distanciaKm" o "precio", se ignoran: el DTO no los contempla y el backend calcula.
        when(despachoService.cotizar(any())).thenReturn(cotizacion());

        mockMvc.perform(post("/api/v1/despacho/cotizacion").with(como("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"direccion\":\"Calle 1\",\"latitud\":-33.4,\"longitud\":-70.6,\"distanciaKm\":0,\"precio\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(3500));
    }
}
