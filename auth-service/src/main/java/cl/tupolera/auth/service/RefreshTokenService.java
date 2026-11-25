package cl.tupolera.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.tupolera.auth.config.JwtProperties;
import cl.tupolera.auth.exception.ApiException;
import cl.tupolera.auth.model.RefreshToken;
import cl.tupolera.auth.repository.RefreshTokenRepository;

/**
 * Administra los refresh tokens: se generan aleatorios, se guardan solo como hash SHA-256,
 * se rotan en cada renovación y se revocan al cerrar sesión.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties props;

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties props) {
        this.repository = repository;
        this.props = props;
    }

    public record Rotacion(Long usuarioId, String nuevoRefreshToken) {
    }

    /** Crea un refresh token nuevo y devuelve su valor original (única vez que existe en claro). */
    @Transactional
    public String crear(Long usuarioId) {
        byte[] aleatorio = new byte[32];
        RANDOM.nextBytes(aleatorio);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);

        repository.save(new RefreshToken(
                usuarioId,
                hash(valor),
                LocalDateTime.now().plusDays(props.refreshTokenDays())));

        return valor;
    }

    /**
     * Valida el refresh token y lo reemplaza por uno nuevo (rotación).
     * Si se presenta un token ya revocado se asume robo o reutilización y se revocan todos
     * los tokens del usuario. noRollbackFor mantiene esa revocación aunque se responda con error.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Rotacion rotar(String valor) {
        RefreshToken actual = repository.findByTokenHash(hash(valor))
                .orElseThrow(() -> ApiException.unauthorized("El refresh token no es válido."));

        if (actual.isRevocado()) {
            repository.revocarTodosDelUsuario(actual.getUsuarioId());
            throw ApiException.unauthorized("El refresh token no es válido.");
        }

        if (actual.estaExpirado()) {
            throw ApiException.unauthorized("El refresh token expiró. Inicia sesión nuevamente.");
        }

        actual.setRevocado(true);
        repository.save(actual);

        return new Rotacion(actual.getUsuarioId(), crear(actual.getUsuarioId()));
    }

    /** Cierra la sesión: invalida el refresh token entregado (si existe). */
    @Transactional
    public void revocar(String valor) {
        repository.findByTokenHash(hash(valor)).ifPresent(token -> {
            token.setRevocado(true);
            repository.save(token);
        });
    }

    @Transactional
    public void revocarTodos(Long usuarioId) {
        repository.revocarTodosDelUsuario(usuarioId);
    }

    static String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
