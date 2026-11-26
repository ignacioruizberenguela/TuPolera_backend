package cl.tupolera.sales.client.dto;

import java.util.List;

/** Cuerpo de error de las operaciones internas de catalog-service (solo los campos usados). */
public record ErrorCatalogo(String message, List<ErrorItemInventario> errores) {
}
