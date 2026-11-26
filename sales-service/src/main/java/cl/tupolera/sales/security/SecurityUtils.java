package cl.tupolera.sales.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import cl.tupolera.sales.exception.ApiException;

/** Lee la identidad del token ya verificado (nunca de parámetros ni del cuerpo de la solicitud). */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** Id del usuario autenticado: es el claim "sub" del JWT emitido por auth-service. */
    public static Long idUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            try {
                return Long.valueOf(jwt.getSubject());
            } catch (NumberFormatException e) {
                throw ApiException.unauthorized("El token no contiene un usuario válido.");
            }
        }
        throw ApiException.unauthorized("Debes iniciar sesión con un token válido.");
    }
}
