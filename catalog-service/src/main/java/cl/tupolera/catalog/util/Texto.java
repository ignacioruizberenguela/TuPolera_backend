package cl.tupolera.catalog.util;

import java.text.Normalizer;

public final class Texto {

    private Texto() {
    }

    /** "Tipo de manga" -> "tipo_de_manga" (identificador interno estable). */
    public static String aIdentificador(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    public static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
