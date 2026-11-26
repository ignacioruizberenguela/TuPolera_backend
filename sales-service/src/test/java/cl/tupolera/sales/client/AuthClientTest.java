package cl.tupolera.sales.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import cl.tupolera.sales.client.dto.UsuarioAuth;
import cl.tupolera.sales.exception.ApiException;

class AuthClientTest {

    private static final String BASE = "http://auth.test";
    private static final String TOKEN = "eyJ.token.de.prueba";

    private MockRestServiceServer server;
    private AuthClient client;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AuthClient(builder.build());
    }

    private static String usuario(boolean activo) {
        return """
                {"id":7,"run":"12345678-5","nombre":"Ana","apellidos":"Pérez Soto","correo":"ana@correo.cl",
                 "rol":"CLIENTE","activo":%s,"direccion":"x"}""".formatted(activo);
    }

    @Test
    void enviaElTokenDelUsuarioYMapeaSusDatos() {
        server.expect(requestTo(BASE + "/api/v1/auth/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andRespond(withSuccess(usuario(true), MediaType.APPLICATION_JSON));

        UsuarioAuth u = client.obtenerUsuarioActual(TOKEN);

        assertThat(u.id()).isEqualTo(7L);
        assertThat(u.nombreCompleto()).isEqualTo("Ana Pérez Soto");
        assertThat(u.correo()).isEqualTo("ana@correo.cl");
        assertThat(u.run()).isEqualTo("12345678-5");
        server.verify();
    }

    @Test
    void tokenRechazadoDevuelve401() {
        server.expect(requestTo(BASE + "/api/v1/auth/me")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.obtenerUsuarioActual(TOKEN))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getMessage()).doesNotContain(TOKEN);
                });
    }

    @Test
    void usuarioInactivoDevuelve401() {
        server.expect(requestTo(BASE + "/api/v1/auth/me"))
                .andRespond(withSuccess(usuario(false), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.obtenerUsuarioActual(TOKEN))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void authCaidoDevuelve503SinFiltrarElToken() {
        server.expect(requestTo(BASE + "/api/v1/auth/me")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.obtenerUsuarioActual(TOKEN))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getMessage()).doesNotContain(TOKEN);
                });
    }

    @Test
    void tiempoAgotadoDevuelve503() {
        server.expect(requestTo(BASE + "/api/v1/auth/me"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.obtenerUsuarioActual(TOKEN))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
