package cl.tupolera.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Clave compartida entre servicios para las operaciones internas (/internal/**).
 * Se lee de la variable de entorno INTERNAL_API_KEY. Si no está definida (o es demasiado corta),
 * las operaciones internas quedan deshabilitadas: toda solicitud a /internal responde 401.
 */
@ConfigurationProperties(prefix = "app.internal")
public record InternalApiProperties(String apiKey) {

    /** Largo mínimo para considerar que la clave es utilizable. */
    public static final int LARGO_MINIMO = 32;

    public boolean estaConfigurada() {
        return apiKey != null && apiKey.length() >= LARGO_MINIMO;
    }
}
