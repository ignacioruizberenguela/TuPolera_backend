package cl.tupolera.catalog.model;

/** Tipos de operación de inventario registradas para garantizar la idempotencia. */
public enum TipoMovimiento {
    /** Descuento de stock por una compra. */
    DESCUENTO,
    /** Reposición de stock. */
    REPOSICION,
    /** Reversión (compensación) de un descuento anterior. */
    REVERSION
}
