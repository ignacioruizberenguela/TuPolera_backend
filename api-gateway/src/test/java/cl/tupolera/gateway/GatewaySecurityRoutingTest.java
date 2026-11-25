
package cl.tupolera.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Pruebas del Gateway: seguridad (401), CORS y enrutamiento.
 * Los microservicios NO están levantados: si la solicitud pasa la seguridad
 * y la ruta existe, el Gateway responde con un error de conexión (5xx),
 * nunca 401/403/404.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.jwt.secret=" + GatewaySecurityRoutingTest.SECRET,
                "app.jwt.issuer=tupolera-auth",
                "app.cors.allowed-origins=http://localhost:5173",
                "AUTH_SERVICE_URL=http://localhost:59991",
                "CATALOG_SERVICE_URL=http://localhost:59992",
                "SALES_SERVICE_URL=http://localhost:59993"
        })
class GatewaySecurityRoutingTest {

    static final String SECRET = "clave-de-pruebas-del-gateway-de-32-bytes-minimo!!";

    private WebTestClient client;

    @LocalServerPort
    private int port;

    @BeforeEach
    void configurarCliente() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    private String tokenValido(String rol) {
        SecretKey key = new SecretKeySpec(
                SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));

        Instant ahora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("tupolera-auth")
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(300))
                .subject("1")
                .claim("rol", rol)
                .build();

        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        )).getTokenValue();
    }

    private static void noEsRechazoNiRutaInexistente(Integer status) {
        assertThat(status).isNotIn(401, 403, 404);
    }

    @Test
    void rutaProtegidaSinTokenResponde401() {
        client.get().uri("/api/v1/boletas")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void tokenInvalidoResponde401() {
        client.get().uri("/api/v1/boletas")
                .header("Authorization", "Bearer token.invalido.firma")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void tokenValidoPasaLaSeguridadYLaRutaExiste() {
        client.get().uri("/api/v1/boletas")
                .header("Authorization", "Bearer " + tokenValido("ADMINISTRADOR"))
                .exchange()
                .expectStatus().value(
                        GatewaySecurityRoutingTest::noEsRechazoNiRutaInexistente
                );
    }

    @Test
    void catalogoEsPublicoParaLectura() {
        client.get().uri("/api/v1/productos")
                .exchange()
                .expectStatus().value(
                        GatewaySecurityRoutingTest::noEsRechazoNiRutaInexistente
                );
    }

    @Test
    void productosCriticosRequierenToken() {
        client.get().uri("/api/v1/productos/criticos")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void escrituraDeProductosRequiereToken() {
        client.post().uri("/api/v1/productos")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void loginEsPublicoYSeEnrutaAAuthService() {
        client.post().uri("/api/v1/auth/login")
                .exchange()
                .expectStatus().value(
                        GatewaySecurityRoutingTest::noEsRechazoNiRutaInexistente
                );
    }

    @Test
    void preflightCorsConOrigenPermitido() {
        client.options().uri("/api/v1/productos")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(
                        "Access-Control-Allow-Origin",
                        "http://localhost:5173"
                );
    }

    @Test
    void preflightCorsConOrigenNoPermitidoSeRechaza() {
        client.options().uri("/api/v1/productos")
                .header("Origin", "http://sitio-malicioso.example")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isForbidden();
    }
}
