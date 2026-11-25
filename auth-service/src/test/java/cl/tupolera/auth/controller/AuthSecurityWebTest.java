package cl.tupolera.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cl.tupolera.auth.config.SecurityConfig;
import cl.tupolera.auth.security.ApiSecurityHandlers;
import cl.tupolera.auth.service.AuthService;
import cl.tupolera.auth.service.UsuarioService;

/** Verifica 401, 403, 200 y validaciones 400 sin base de datos (capa web + seguridad). */
@WebMvcTest(controllers = { UsuarioController.class, AuthController.class })
@Import({ SecurityConfig.class, ApiSecurityHandlers.class })
class AuthSecurityWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void listarUsuariosSinTokenResponde401EnJson() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void listarUsuariosComoClienteOVendedorResponde403() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/v1/usuarios")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarUsuariosComoAdministradorResponde200() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/usuarios")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
                .andExpect(status().isOk());
    }

    @Test
    void registroConDatosInvalidosResponde400ConDetalle() throws Exception {
        String cuerpo = """
                {"run":"12.345.678-9","nombre":"","correo":"no-es-correo","password":"123",
                 "idRegion":1,"idComuna":1}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    void registroEsPublicoYNoRequiereToken() throws Exception {
        when(authService.registrar(any())).thenReturn(null);
        String cuerpo = """
                {"run":"12.345.678-5","nombre":"Juan","correo":"juan@gmail.com","password":"123456",
                 "idRegion":1,"idComuna":1}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isCreated());
    }

    @Test
    void perfilSinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
