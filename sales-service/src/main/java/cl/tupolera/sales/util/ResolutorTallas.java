package cl.tupolera.sales.util;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import cl.tupolera.sales.exception.ApiException;

/**
 * Regla aprobada para las tallas: primero coincidencia exacta; si no existe, una única coincidencia sin
 * distinguir mayúsculas; si hay varias posibles, 400 (nunca se adivina).
 */
public final class ResolutorTallas {

    private ResolutorTallas() {
    }

    /** @return el nombre EXACTO de la talla en el catálogo. */
    public static String resolver(String solicitada, Collection<String> disponibles) {
        if (solicitada == null || solicitada.isBlank()) {
            throw ApiException.badRequest("La talla es obligatoria.");
        }

        if (disponibles.contains(solicitada)) {
            return solicitada;
        }

        String buscada = solicitada.strip().toLowerCase(Locale.ROOT);
        List<String> coincidencias = disponibles.stream()
                .filter(t -> t != null && t.strip().toLowerCase(Locale.ROOT).equals(buscada))
                .distinct()
                .toList();

        if (coincidencias.size() == 1) {
            return coincidencias.get(0);
        }
        if (coincidencias.size() > 1) {
            throw ApiException.badRequest("La talla \"" + solicitada
                    + "\" es ambigua: coincide con varias tallas del producto. Indica la talla exacta.");
        }
        throw ApiException.badRequest("La talla \"" + solicitada + "\" no existe para este producto.");
    }
}
