package cl.tupolera.sales.client;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros para llamar a los otros servicios (siempre directo a su puerto, nunca por el Gateway).
 * Usa las mismas variables de entorno que el resto del proyecto: CATALOG_SERVICE_URL, AUTH_SERVICE_URL e
 * INTERNAL_API_KEY. La clave interna nunca se escribe en logs ni en mensajes de error.
 */
@ConfigurationProperties(prefix = "app.clientes")
public record ClientesProperties(
        String catalogoUrl,
        String authUrl,
        String internalApiKey,
        Duration timeoutConexion,
        Duration timeoutLectura) {

    public ClientesProperties {
        if (timeoutConexion == null) {
            timeoutConexion = Duration.ofSeconds(2);
        }
        if (timeoutLectura == null) {
            timeoutLectura = Duration.ofSeconds(5);
        }
    }
}
