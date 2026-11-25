package cl.tupolera.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.tupolera.auth.config.JwtProperties;
import cl.tupolera.auth.exception.ApiException;
import cl.tupolera.auth.model.RefreshToken;
import cl.tupolera.auth.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;

    @BeforeEach
    void preparar() {
        service = new RefreshTokenService(repository,
                new JwtProperties("x".repeat(32), "tupolera-auth", 15, 7));
    }

    @Test
    void guardaSoloElHashDelToken() {
        String valor = service.crear(5L);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());

        RefreshToken guardado = captor.getValue();
        assertThat(guardado.getTokenHash()).isNotEqualTo(valor);
        assertThat(guardado.getTokenHash()).isEqualTo(RefreshTokenService.hash(valor)).hasSize(64);
        assertThat(guardado.getUsuarioId()).isEqualTo(5L);
        assertThat(guardado.estaExpirado()).isFalse();
    }

    @Test
    void rotarRevocaElTokenUsadoYEmiteUnoNuevo() {
        RefreshToken actual = new RefreshToken(5L, RefreshTokenService.hash("viejo"),
                LocalDateTime.now().plusDays(1));
        when(repository.findByTokenHash(RefreshTokenService.hash("viejo"))).thenReturn(Optional.of(actual));

        RefreshTokenService.Rotacion rotacion = service.rotar("viejo");

        assertThat(actual.isRevocado()).isTrue();
        assertThat(rotacion.usuarioId()).isEqualTo(5L);
        assertThat(rotacion.nuevoRefreshToken()).isNotBlank().isNotEqualTo("viejo");
    }

    @Test
    void reutilizarUnTokenRevocadoRevocaTodasLasSesionesDelUsuario() {
        RefreshToken usado = new RefreshToken(5L, RefreshTokenService.hash("usado"),
                LocalDateTime.now().plusDays(1));
        usado.setRevocado(true);
        when(repository.findByTokenHash(RefreshTokenService.hash("usado"))).thenReturn(Optional.of(usado));

        assertThatThrownBy(() -> service.rotar("usado"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus().value()).isEqualTo(401);

        verify(repository).revocarTodosDelUsuario(5L);
    }

    @Test
    void rechazaTokensInexistentesOExpirados() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.rotar("no-existe")).isInstanceOf(ApiException.class);

        RefreshToken vencido = new RefreshToken(5L, RefreshTokenService.hash("vencido"),
                LocalDateTime.now().minusMinutes(1));
        when(repository.findByTokenHash(RefreshTokenService.hash("vencido"))).thenReturn(Optional.of(vencido));
        assertThatThrownBy(() -> service.rotar("vencido")).isInstanceOf(ApiException.class);
    }
}
