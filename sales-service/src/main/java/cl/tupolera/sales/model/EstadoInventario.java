package cl.tupolera.sales.model;

/**
 * Situación del descuento de inventario de una compra.
 * NO_APLICA: nunca se tocó el stock. PENDIENTE: por descontar. DESCONTADO: confirmado por catalog-service.
 * REVERTIDO: se descontó y se devolvió. INCIERTO: no se sabe si catalog-service lo aplicó.
 */
public enum EstadoInventario { NO_APLICA, PENDIENTE, DESCONTADO, REVERTIDO, INCIERTO }
