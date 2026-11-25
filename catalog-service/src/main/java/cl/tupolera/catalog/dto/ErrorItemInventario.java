package cl.tupolera.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Problema detectado en un ítem. Códigos: PRODUCTO_NO_ENCONTRADO, PRODUCTO_INACTIVO,
 * VARIANTE_NO_ENCONTRADA, STOCK_INSUFICIENTE, LIMITE_STOCK, CONFLICTO_IDEMPOTENCIA, REFERENCIA_REVERTIDA,
 * DESCUENTO_NO_ENCONTRADO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorItemInventario(Long productoId, String talla, String codigo, String mensaje, Integer disponible) {
}
