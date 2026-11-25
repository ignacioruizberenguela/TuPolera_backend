package cl.tupolera.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del JWT. El secreto se obtiene SIEMPRE desde una variable de entorno
 * (JWT_SECRET) y debe ser el mismo que usa auth-service para firmar los tokens.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer) {
}
