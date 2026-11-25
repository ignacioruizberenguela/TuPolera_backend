package cl.tupolera.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import cl.tupolera.auth.dto.AuthResponse;
import cl.tupolera.auth.dto.LoginRequest;
import cl.tupolera.auth.dto.RefreshRequest;
import cl.tupolera.auth.dto.UsuarioResponse;
import cl.tupolera.auth.exception.ApiException;
import cl.tupolera.auth.model.Rol;
import cl.tupolera.auth.model.Usuario;
import cl.tupolera.auth.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UsuarioMapper mapper;

    private AuthService service;

    @BeforeEach
    void preparar() {
        service = new AuthService(usuarioRepository, usuarioService, passwordEncoder, jwtService,
                refreshTokenService, mapper);
    }

    private Usuario usuario(boolean activo) {
        Usuario u = new Usuario();
        ReflectionTestUtils.setField(u, "id", 1L);
        u.setCorreo("juan@gmail.com");
        u.setPassword("hash-bcrypt");
        u.setRol(Rol.CLIENTE);
        u.setActivo(activo);
        return u;
    }

    private static int status(Throwable t) {
        return ((ApiException) t).getStatus().value();
    }

    @Test
    void loginCorrectoEntregaAccessYRefreshToken() {
        Usuario u = usuario(true);
        when(usuarioRepository.findByCorreoIgnoreCase("juan@gmail.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("123456", "hash-bcrypt")).thenReturn(true);
        when(jwtService.generarAccessToken(u)).thenReturn("jwt");
        when(jwtService.segundosDeVidaAccessToken()).thenReturn(900L);
        when(refreshTokenService.crear(1L)).thenReturn("refresh");
        when(mapper.toResponse(u)).thenReturn(
                new UsuarioResponse(1L, "36322977", "Juan", null, "juan@gmail.com", null, null, 1L, "R", 1L, "C",
                        Rol.CLIENTE, true, null));

        AuthResponse respuesta = service.login(new LoginRequest(" juan@gmail.com ", "123456"));

        assertThat(respuesta.accessToken()).isEqualTo("jwt");
        assertThat(respuesta.refreshToken()).isEqualTo("refresh");
        assertThat(respuesta.tokenType()).isEqualTo("Bearer");
        assertThat(respuesta.expiresIn()).isEqualTo(900L);
    }

    @Test
    void loginConContrasenaIncorrectaResponde401SinGenerarTokens() {
        Usuario u = usuario(true);
        when(usuarioRepository.findByCorreoIgnoreCase("juan@gmail.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("mala", "hash-bcrypt")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("juan@gmail.com", "mala")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(status(e)).isEqualTo(401));

        verify(refreshTokenService, never()).crear(any());
    }

    @Test
    void loginDeUsuarioInactivoOInexistenteResponde401() {
        when(usuarioRepository.findByCorreoIgnoreCase("inactivo@gmail.com")).thenReturn(Optional.of(usuario(false)));
        when(usuarioRepository.findByCorreoIgnoreCase("nadie@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("inactivo@gmail.com", "123456")))
                .satisfies(e -> assertThat(status(e)).isEqualTo(401));
        assertThatThrownBy(() -> service.login(new LoginRequest("nadie@gmail.com", "123456")))
                .satisfies(e -> assertThat(status(e)).isEqualTo(401));
    }

    @Test
    void refreshDeUsuarioDesactivadoRevocaSusSesionesYResponde401() {
        Usuario u = usuario(false);
        when(refreshTokenService.rotar("r1")).thenReturn(new RefreshTokenService.Rotacion(1L, "r2"));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.refresh(new RefreshRequest("r1")))
                .satisfies(e -> assertThat(status(e)).isEqualTo(401));

        verify(refreshTokenService).revocarTodos(1L);
    }

    @Test
    void logoutInvalidaElRefreshToken() {
        service.logout(new RefreshRequest("r1"));

        verify(refreshTokenService).revocar("r1");
    }
}
