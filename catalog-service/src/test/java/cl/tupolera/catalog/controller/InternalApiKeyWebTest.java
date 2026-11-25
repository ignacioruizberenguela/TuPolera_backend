package cl.tupolera.catalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import cl.tupolera.catalog.config.InternalSecurityConfig;
import cl.tupolera.catalog.config.SecurityConfig;
import cl.tupolera.catalog.dto.ConsultaInventarioResponse;
import cl.tupolera.catalog.dto.ResultadoInventarioResponse;
import cl.tupolera.catalog.security.ApiSecurityHandlers;
import cl.tupolera.catalog.service.InventarioService;

/** Protección de /internal/** con X-Internal-Key y validación de las solicitudes (capa web, sin base de datos). */
@WebMvcTest(controllers = { InventarioInternoController.class, ProductoController.class })
@Import({ InternalSecurityConfig.class, SecurityConfig.class, ApiSecurityHandlers.class })
@TestPropertySource(properties = "app.internal.api-key=" + InternalApiKeyWebTest.CLAVE)
class InternalApiKeyWebTest {

    static final String CLAVE = "clave-interna-de-pruebas-de-32-caracteres-minimo";
    private static final String REF = "3f2b8c9e-6a41-4d52-9c1e-0a7b5d2e8f10";

    private static final String CONSULTA = "{\"productoIds\":[1,2]}";
    private static final String DESCUENTO = """
            {"referencia":"%s","items":[{"productoId":1,"talla":"M","cantidad":2}]}
            """.formatted(REF);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private InventarioService inventarioService;
    @MockitoBean private cl.tupolera.catalog.service.ProductoService productoService;
    @MockitoBean private JwtDecoder jwtDecoder;

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
    }

    /* ------------------------- la clave ------------------------- */

    @Test
    void sinClaveResponde401() throws Exception {
        mockMvc.perform(post("/internal/inventario/consulta").contentType(MediaType.APPLICATION_JSON).content(CONSULTA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(inventarioService);
    }

    @Test
    void conClaveIncorrectaResponde401() throws Exception {
        for (String incorrecta : new String[] { "clave-equivocada", "", " ", CLAVE + "x", CLAVE.toUpperCase() }) {
            mockMvc.perform(post("/internal/inventario/consulta")
                    .header("X-Internal-Key", incorrecta)
                    .contentType(MediaType.APPLICATION_JSON).content(CONSULTA))
                    .andExpect(status().isUnauthorized());
        }

        verifyNoInteractions(inventarioService);
    }

    @Test
    void conLaClaveCorrectaPasa() throws Exception {
        when(inventarioService.consultar(any())).thenReturn(new ConsultaInventarioResponse(List.of(), List.of(2L)));

        mockMvc.perform(post("/internal/inventario/consulta")
                .header("X-Internal-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON).content(CONSULTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noEncontrados[0]").value(2));
    }

    @Test
    void laRespuestaDeErrorNuncaIncluyeLaClave() throws Exception {
        mockMvc.perform(post("/internal/inventario/consulta")
                .header("X-Internal-Key", CLAVE + "mal")
                .contentType(MediaType.APPLICATION_JSON).content(CONSULTA))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(CLAVE))));
    }

    /* ------------------------- el JWT no sirve aquí (y la clave no sirve fuera) ------------------------- */

    @Test
    void unJwtDeAdministradorSinLaClaveNoDaAccesoAInternal() throws Exception {
        mockMvc.perform(post("/internal/inventario/descuento").with(admin())
                .contentType(MediaType.APPLICATION_JSON).content(DESCUENTO))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/internal/inventario/descuento/" + REF + "/reversion").with(admin()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(inventarioService);
    }

    @Test
    void laClaveInternaNoDaAccesoALosEndpointsAdministrativosDelCatalogo() throws Exception {
        mockMvc.perform(get("/api/v1/productos/criticos").header("X-Internal-Key", CLAVE))
                .andExpect(status().isUnauthorized());
    }

    /* ------------------------- los cuatro endpoints ------------------------- */

    @Test
    void descuentoReposicionYReversionRequierenLaClave() throws Exception {
        String reposicion = "{\"claveIdempotencia\":\"" + REF + "\",\"items\":[{\"productoId\":1,\"talla\":\"M\",\"cantidad\":3}]}";

        mockMvc.perform(post("/internal/inventario/descuento").contentType(MediaType.APPLICATION_JSON).content(DESCUENTO))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/internal/inventario/reposicion").contentType(MediaType.APPLICATION_JSON).content(reposicion))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/internal/inventario/descuento/" + REF + "/reversion"))
                .andExpect(status().isUnauthorized());

        when(inventarioService.descontar(any())).thenReturn(new ResultadoInventarioResponse(REF, "DESCUENTO", false, List.of()));
        when(inventarioService.reponer(any())).thenReturn(new ResultadoInventarioResponse(REF, "REPOSICION", false, List.of()));
        when(inventarioService.revertirDescuento(REF)).thenReturn(new ResultadoInventarioResponse(REF, "REVERSION", false, List.of()));

        mockMvc.perform(post("/internal/inventario/descuento").header("X-Internal-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON).content(DESCUENTO))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("DESCUENTO"));
        mockMvc.perform(post("/internal/inventario/reposicion").header("X-Internal-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON).content(reposicion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("REPOSICION"));
        mockMvc.perform(post("/internal/inventario/descuento/" + REF + "/reversion").header("X-Internal-Key", CLAVE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("REVERSION"));

        verify(inventarioService).revertirDescuento(REF);
    }

    /* ------------------------- cantidades y datos inválidos ------------------------- */

    private void descuentoInvalido(String cuerpo) throws Exception {
        mockMvc.perform(post("/internal/inventario/descuento").header("X-Internal-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    void cantidadesInvalidasResponden400() throws Exception {
        String base = "{\"referencia\":\"" + REF + "\",\"items\":[{\"productoId\":1,\"talla\":\"M\",\"cantidad\":%s}]}";

        descuentoInvalido(base.formatted("0"));
        descuentoInvalido(base.formatted("-3"));
        descuentoInvalido(base.formatted("10001"));
        descuentoInvalido(base.formatted("null"));
    }

    @Test
    void referenciaItemsOTallaInvalidosResponden400() throws Exception {
        descuentoInvalido("{\"referencia\":\"corta\",\"items\":[{\"productoId\":1,\"talla\":\"M\",\"cantidad\":1}]}");
        descuentoInvalido("{\"items\":[{\"productoId\":1,\"talla\":\"M\",\"cantidad\":1}]}");
        descuentoInvalido("{\"referencia\":\"" + REF + "\",\"items\":[]}");
        descuentoInvalido("{\"referencia\":\"" + REF + "\",\"items\":[{\"productoId\":1,\"talla\":\"\",\"cantidad\":1}]}");
        descuentoInvalido("{\"referencia\":\"" + REF + "\",\"items\":[{\"talla\":\"M\",\"cantidad\":1}]}");
    }

    @Test
    void consultaSinProductosResponde400() throws Exception {
        mockMvc.perform(post("/internal/inventario/consulta").header("X-Internal-Key", CLAVE)
                .contentType(MediaType.APPLICATION_JSON).content("{\"productoIds\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
