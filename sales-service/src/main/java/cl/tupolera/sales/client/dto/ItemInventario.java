package cl.tupolera.sales.client.dto;

/** Variante y cantidad a descontar (talla ya resuelta con el nombre exacto del catálogo). */
public record ItemInventario(Long productoId, String talla, Integer cantidad) {
}
