package cl.tupolera.catalog.dto;

import java.util.List;

/**
 * Resultado de una operación de inventario. "repetida" es true cuando la referencia ya se había aplicado
 * antes: se devuelve el resultado original sin volver a modificar el stock.
 * "stockActual" es el stock de la variante justo después de aplicar la operación.
 */
public record ResultadoInventarioResponse(String referencia, String tipo, boolean repetida,
        List<ItemResultadoResponse> items) {
}
