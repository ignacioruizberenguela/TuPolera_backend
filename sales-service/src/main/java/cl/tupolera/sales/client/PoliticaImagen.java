package cl.tupolera.sales.client;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Regla aprobada para las imágenes de las boletas: se guarda una copia de la imagen del catálogo solo si pesa
 * 1 MB o menos; si pesa más (o no hay imagen) la boleta se guarda sin imagen.
 *
 * El tamaño se mide sobre el texto que catalog-service entrega (nombre de archivo o data URL en base64), que es
 * exactamente lo que se almacenaría en la boleta.
 */
public final class PoliticaImagen {

    public static final int MAXIMO_BYTES = 1_048_576;

    private PoliticaImagen() {
    }

    public static Optional<String> aplicar(String imagen) {
        if (imagen == null || imagen.isBlank()) {
            return Optional.empty();
        }
        if (imagen.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            return Optional.empty();
        }
        return Optional.of(imagen);
    }
}
