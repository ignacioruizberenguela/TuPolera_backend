package cl.tupolera.sales.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros para VERIFICAR los JWT emitidos por auth-service.
 * Usa las mismas variables de entorno que auth-service y el Gateway (JWT_SECRET y JWT_ISSUER).
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer) {
}
