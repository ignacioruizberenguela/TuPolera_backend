package cl.tupolera.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import cl.tupolera.auth.config.JwtConfig;
import cl.tupolera.auth.config.JwtProperties;
import cl.tupolera.auth.model.Rol;
import cl.tupolera.auth.model.Usuario;

import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class JwtServiceTest {

    private static final String SECRET = "secreto-de-pruebas-para-jwt-de-32-bytes-o-mas!!";

    private JwtProperties props;
    private JwtConfig config;
    private JwtService jwtService;
    private JwtDecoder decoder;

    @BeforeEach
    void preparar() {
        props = new JwtProperties(SECRET, "https://tupolera.example", 15, 7);
        config = new JwtConfig();
        SecretKey key = config.jwtSecretKey(props);
        JwtEncoder encoder = config.jwtEncoder(key);
        decoder = config.jwtDecoder(key, props);
        jwtService = new JwtService(encoder, props);
    }

    private Usuario usuario(Rol rol) {
        Usuario u = new Usuario();
        ReflectionTestUtils.setField(u, "id", 7L);
        u.setCorreo("juan@gmail.com");
        u.setNombre("Juan");
        u.setRol(rol);
        return u;
    }

    @Test
    void generaTokenFirmadoConIdentidadYRol() {
        String token = jwtService.generarAccessToken(usuario(Rol.VENDEDOR));

        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.<String>getClaim("rol")).isEqualTo("VENDEDOR");
        assertThat(jwt.<String>getClaim("correo")).isEqualTo("juan@gmail.com");
        assertThat(jwt.getIssuer().toString()).isEqualTo("https://tupolera.example");
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void rechazaUnTokenAlteradoOFirmadoConOtraClave() {
        String token = jwtService.generarAccessToken(usuario(Rol.CLIENTE));
        String alterado = token.substring(0, token.length() - 3) + "abc";

        assertThatThrownBy(() -> decoder.decode(alterado)).isInstanceOf(BadJwtException.class);

        JwtProperties otra = new JwtProperties("otro-secreto-distinto-de-32-bytes-o-mas-aqui!!", "https://tupolera.example", 15, 7);
        JwtService otroServicio = new JwtService(config.jwtEncoder(config.jwtSecretKey(otra)), otra);
        String conOtraClave = otroServicio.generarAccessToken(usuario(Rol.ADMINISTRADOR));

        assertThatThrownBy(() -> decoder.decode(conOtraClave)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void rechazaUnTokenExpirado() {
        SecretKey key = config.jwtSecretKey(props);
        JwtEncoder encoder = config.jwtEncoder(key);

        Instant ahora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .issuedAt(ahora.minusSeconds(1200))
                .expiresAt(ahora.minusSeconds(600))
                .subject("7")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = encoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(BadJwtException.class);
    }

    @Test
    void exigeUnSecretoDeAlMenos32Bytes() {
        JwtProperties corto = new JwtProperties("corto", "tupolera-auth", 15, 7);

        assertThatThrownBy(() -> config.jwtSecretKey(corto)).isInstanceOf(IllegalStateException.class);
    }
}
