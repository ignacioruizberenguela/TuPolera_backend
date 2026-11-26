package cl.tupolera.sales.model;

/** Estado de la compra. APROBADA solo existe cuando el inventario ya está DESCONTADO (lo exige el esquema). */
public enum EstadoBoleta { PENDIENTE, APROBADA, RECHAZADA }
