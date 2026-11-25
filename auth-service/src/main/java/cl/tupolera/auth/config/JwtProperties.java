package cl.tupolera.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros del JWT. El secreto viene siempre de la variable de entorno JWT_SECRET
 * (nunca está escrito en el código). El mismo secreto debe configurarse en el Gateway,
 * catalog-service y sales-service para que puedan verificar los tokens.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, long accessTokenMinutes, long refreshTokenDays) {
}
