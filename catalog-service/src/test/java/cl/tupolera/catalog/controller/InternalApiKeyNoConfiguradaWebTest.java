package cl.tupolera.catalog.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cl.tupolera.catalog.config.InternalSecurityConfig;
import cl.tupolera.catalog.config.SecurityConfig;
import cl.tupolera.catalog.security.ApiSecurityHandlers;
import cl.tupolera.catalog.service.InventarioService;

/**
 * Sin INTERNAL_API_KEY configurada (o demasiado corta), las operaciones internas quedan deshabilitadas:
 * ninguna solicitud pasa, ni siquiera enviando una cabecera.
 */
@WebMvcTest(controllers = InventarioInternoController.class)
@Import({ InternalSecurityConfig.class, SecurityConfig.class, ApiSecurityHandlers.class })
@TestPropertySource(properties = "app.internal.api-key=")
class InternalApiKeyNoConfiguradaWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private InventarioService inventarioService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void sinClaveConfiguradaTodoInternalResponde401() throws Exception {
        for (String enviada : new String[] { "", "cualquier-valor", "null" }) {
            mockMvc.perform(post("/internal/inventario/consulta")
                    .header("X-Internal-Key", enviada)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"productoIds\":[1]}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/internal/inventario/consulta")
                .contentType(MediaType.APPLICATION_JSON).content("{\"productoIds\":[1]}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(inventarioService);
    }
}

/** Una clave demasiado corta también se considera "no configurada". */
@WebMvcTest(controllers = InventarioInternoController.class)
@Import({ InternalSecurityConfig.class, SecurityConfig.class, ApiSecurityHandlers.class })
@TestPropertySource(properties = "app.internal.api-key=corta123")
class InternalApiKeyDemasiadoCortaWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private InventarioService inventarioService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void unaClaveDeMenosDe32CaracteresNoHabilitaLasOperaciones() throws Exception {
        mockMvc.perform(post("/internal/inventario/consulta")
                .header("X-Internal-Key", "corta123")
                .contentType(MediaType.APPLICATION_JSON).content("{\"productoIds\":[1]}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(inventarioService);
    }
}
