package cl.tupolera.catalog.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Lectura del rol desde el token ya verificado (nunca desde parámetros de la solicitud). */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** ADMINISTRADOR y VENDEDOR pueden consultar también los registros inactivos. */
    public static boolean puedeVerInactivos() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        for (GrantedAuthority authority : auth.getAuthorities()) {
            String rol = authority.getAuthority();
            if ("ROLE_ADMINISTRADOR".equals(rol) || "ROLE_VENDEDOR".equals(rol)) {
                return true;
            }
        }
        return false;
    }
}
