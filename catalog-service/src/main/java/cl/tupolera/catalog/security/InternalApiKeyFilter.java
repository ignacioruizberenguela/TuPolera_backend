package cl.tupolera.catalog.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import cl.tupolera.catalog.config.InternalApiProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica las solicitudes internas entre servicios mediante la cabecera X-Internal-Key.
 *
 * - No es un bean de Spring a propósito: se agrega solo a la cadena de /internal/** (si fuera un bean,
 *   Spring Boot lo registraría también para el resto de las rutas).
 * - Compara en tiempo constante (hash SHA-256 de ambos valores) para no filtrar información por tiempos.
 * - Nunca escribe en el log ni devuelve el valor de la clave.
 */
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String CABECERA = "X-Internal-Key";
    public static final String ROL = "SERVICIO_INTERNO";

    private static final Logger log = LoggerFactory.getLogger(InternalApiKeyFilter.class);

    private final byte[] huellaEsperada;
    private final ApiSecurityHandlers handlers;

    public InternalApiKeyFilter(InternalApiProperties props, ApiSecurityHandlers handlers) {
        this.handlers = handlers;

        if (props.estaConfigurada()) {
            this.huellaEsperada = sha256(props.apiKey());
        } else {
            this.huellaEsperada = null;
            log.warn("INTERNAL_API_KEY no está configurada (o tiene menos de {} caracteres): "
                    + "las operaciones internas /internal/** están DESHABILITADAS y responderán 401.",
                    InternalApiProperties.LARGO_MINIMO);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String enviada = request.getHeader(CABECERA);

        if (esValida(enviada)) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "servicio-interno", null, List.of(new SimpleGrantedAuthority("ROLE_" + ROL))));
            chain.doFilter(request, response);
            return;
        }

        handlers.commence(request, response, new BadCredentialsException("Clave interna inválida"));
    }

    private boolean esValida(String enviada) {
        if (huellaEsperada == null || enviada == null || enviada.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(huellaEsperada, sha256(enviada));
    }

    private static byte[] sha256(String valor) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
