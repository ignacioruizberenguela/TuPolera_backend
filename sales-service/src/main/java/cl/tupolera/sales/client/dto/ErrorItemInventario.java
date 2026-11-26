package cl.tupolera.sales.client.dto;

/**
 * Problema de un ítem informado por catalog-service. Códigos conocidos: PRODUCTO_NO_ENCONTRADO,
 * PRODUCTO_INACTIVO, VARIANTE_NO_ENCONTRADA, STOCK_INSUFICIENTE, LIMITE_STOCK, CONFLICTO_IDEMPOTENCIA,
 * REFERENCIA_REVERTIDA, DESCUENTO_NO_ENCONTRADO.
 */
public record ErrorItemInventario(Long productoId, String talla, String codigo, String mensaje, Integer disponible) {
}
