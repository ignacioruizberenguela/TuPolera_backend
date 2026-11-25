package cl.tupolera.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import cl.tupolera.auth.dto.RegistroRequest;
import cl.tupolera.auth.dto.UsuarioActualizarRequest;
import cl.tupolera.auth.exception.ApiException;
import cl.tupolera.auth.model.Comuna;
import cl.tupolera.auth.model.Region;
import cl.tupolera.auth.model.Rol;
import cl.tupolera.auth.model.Usuario;
import cl.tupolera.auth.repository.ComunaRepository;
import cl.tupolera.auth.repository.RegionRepository;
import cl.tupolera.auth.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RegionRepository regionRepository;
    @Mock private ComunaRepository comunaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UsuarioMapper mapper;

    private UsuarioService service;

    @BeforeEach
    void preparar() {
        service = new UsuarioService(usuarioRepository, regionRepository, comunaRepository, passwordEncoder,
                refreshTokenService, mapper);
    }

    private static int status(Throwable t) {
        return ((ApiException) t).getStatus().value();
    }

    private RegistroRequest registro() {
        return new RegistroRequest("12.345.678-5", " Juan ", null, "Juan@Gmail.com", "123456", null, null, 7L, 70L);
    }

    private void ubicacionValida() {
        Region region = mock(Region.class);
        when(region.getId()).thenReturn(7L);
        Comuna comuna = mock(Comuna.class);
        when(regionRepository.findById(7L)).thenReturn(Optional.of(region));
        when(comunaRepository.findByIdAndRegionId(70L, 7L)).thenReturn(Optional.of(comuna));
    }

    @Test
    void registroPublicoCreaSiempreUnClienteConContrasenaEncriptada() {
        ubicacionValida();
        when(passwordEncoder.encode("123456")).thenReturn("$2b$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrarCliente(registro());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();

        assertThat(guardado.getRol()).isEqualTo(Rol.CLIENTE);
        assertThat(guardado.isActivo()).isTrue();
        assertThat(guardado.getRun()).isEqualTo("123456785");
        assertThat(guardado.getCorreo()).isEqualTo("juan@gmail.com");
        assertThat(guardado.getNombre()).isEqualTo("Juan");
        assertThat(guardado.getPassword()).isEqualTo("$2b$hash").isNotEqualTo("123456");
    }

    @Test
    void registroConCorreoDuplicadoResponde409() {
        when(usuarioRepository.existsByCorreoIgnoreCase("juan@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarCliente(registro()))
                .satisfies(e -> assertThat(status(e)).isEqualTo(409));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registroConRunDuplicadoResponde409() {
        when(usuarioRepository.existsByRun("123456785")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarCliente(registro()))
                .satisfies(e -> assertThat(status(e)).isEqualTo(409));
    }

    @Test
    void registroConComunaQueNoPerteneceALaRegionResponde400() {
        Region region = mock(Region.class);
        when(region.getId()).thenReturn(7L);
        when(regionRepository.findById(7L)).thenReturn(Optional.of(region));
        when(comunaRepository.findByIdAndRegionId(70L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarCliente(registro()))
                .satisfies(e -> assertThat(status(e)).isEqualTo(400));
    }

    private Usuario usuarioExistente(Rol rol) {
        Usuario u = new Usuario();
        ReflectionTestUtils.setField(u, "id", 3L);
        u.setRol(rol);
        u.setActivo(true);
        u.setPassword("hash");
        return u;
    }

    private UsuarioActualizarRequest actualizar(Rol rol, boolean activo) {
        return new UsuarioActualizarRequest("79679801", "Admin", null, "adminn@gmail.com", null, null, null,
                7L, 70L, rol, activo);
    }

    @Test
    void unAdministradorNoPuedeCambiarSuPropioRolNiDesactivarse() {
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuarioExistente(Rol.ADMINISTRADOR)));

        assertThatThrownBy(() -> service.actualizar(3L, actualizar(Rol.CLIENTE, true), 3L))
                .satisfies(e -> assertThat(status(e)).isEqualTo(409));
        assertThatThrownBy(() -> service.actualizar(3L, actualizar(Rol.ADMINISTRADOR, false), 3L))
                .satisfies(e -> assertThat(status(e)).isEqualTo(409));
    }

    @Test
    void cambiarElRolDeOtroUsuarioCierraSusSesiones() {
        Usuario u = usuarioExistente(Rol.CLIENTE);
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(u));
        ubicacionValida();
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.actualizar(3L, actualizar(Rol.VENDEDOR, true), 1L);

        assertThat(u.getRol()).isEqualTo(Rol.VENDEDOR);
        verify(refreshTokenService).revocarTodos(3L);
    }

    @Test
    void desactivarMarcaInactivoYRevocaTokens_peroNoPermiteAutodesactivarse() {
        Usuario u = usuarioExistente(Rol.CLIENTE);
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(u));

        service.desactivar(3L, 1L);

        assertThat(u.isActivo()).isFalse();
        verify(refreshTokenService).revocarTodos(3L);

        assertThatThrownBy(() -> service.desactivar(3L, 3L))
                .satisfies(e -> assertThat(status(e)).isEqualTo(409));
    }

    @Test
    void consultarUnUsuarioInexistenteResponde404() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L))
                .satisfies(e -> assertThat(status(e)).isEqualTo(404));
    }
}
