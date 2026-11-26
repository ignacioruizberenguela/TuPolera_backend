package cl.tupolera.sales.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import cl.tupolera.sales.client.dto.UsuarioAuth;
import cl.tupolera.sales.exception.ApiException;

/**
 * Cliente de auth-service. Consulta GET /api/v1/auth/me con el JWT del propio usuario (el mismo que recibió
 * sales-service), que valida que siga activo y entrega los datos para el snapshot del comprador.
 */
@Component
public class AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClient.class);

    private final RestClient rest;

    public AuthClient(@Qualifier("authRestClient") RestClient rest) {
        this.rest = rest;
    }

    /**
     * @param tokenJwt token del usuario (sin el prefijo "Bearer"); nunca se escribe en logs.
     * @throws ApiException 401 si el token no sirve o el usuario está inactivo; 503 si auth-service no responde.
     */
    public UsuarioAuth obtenerUsuarioActual(String tokenJwt) {
        UsuarioAuth usuario;
        try {
            usuario = rest.get()
                    .uri("/api/v1/auth/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenJwt)
                    .retrieve()
                    .body(UsuarioAuth.class);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw ApiException.unauthorized("Tu sesión no es válida o tu usuario está inactivo.");
            }
            throw noDisponible("auth-service respondió " + status + " en /auth/me.", ex);
        } catch (RestClientException ex) {
            throw noDisponible("No se pudo consultar auth-service: " + ex.getClass().getSimpleName(), ex);
        }

        if (usuario == null || usuario.id() == null) {
            throw noDisponible("auth-service devolvió una respuesta vacía en /auth/me.", null);
        }
        if (!usuario.activo()) {
            throw ApiException.unauthorized("Tu sesión no es válida o tu usuario está inactivo.");
        }
        return usuario;
    }

    private static ApiException noDisponible(String detalleInterno, Throwable causa) {
        log.warn("{}", detalleInterno, causa);
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo verificar tu usuario por ahora. Inténtalo nuevamente en unos segundos.");
    }
}
