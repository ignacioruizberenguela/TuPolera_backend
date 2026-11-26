package cl.tupolera.sales.client.dto;

/** Variante (talla) de un producto, con su stock actual en catalog-service. */
public record VarianteCatalogo(Long id, String talla, int stock) {
}
