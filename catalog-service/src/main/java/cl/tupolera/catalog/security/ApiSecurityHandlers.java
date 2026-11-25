package cl.tupolera.catalog.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.tupolera.catalog.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Respuestas JSON para 401 (sin token válido) y 403 (sin permisos) generadas por Spring Security. */
@Component
public class ApiSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiSecurityHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        escribir(request, response, HttpStatus.UNAUTHORIZED, "Debes iniciar sesión con un token válido.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        escribir(request, response, HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción.");
    }

    private void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
            String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse cuerpo = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensaje,
                request.getRequestURI(), null);

        objectMapper.writeValue(response.getOutputStream(), cuerpo);
    }
}
