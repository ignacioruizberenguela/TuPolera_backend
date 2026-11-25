package cl.tupolera.catalog.dto;

import java.time.Instant;
import java.util.List;

/** Error de una operación de inventario: el formato estándar más la lista "errores" con el detalle por ítem. */
public record InventarioErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<ErrorItemInventario> errores) {
}
