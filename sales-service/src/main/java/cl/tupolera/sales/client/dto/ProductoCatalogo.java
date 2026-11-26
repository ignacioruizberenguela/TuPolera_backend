package cl.tupolera.sales.client.dto;

import java.util.List;

/**
 * Producto tal como lo entrega POST /internal/inventario/consulta. "precioFinal" es el precio de venta vigente
 * en CLP, con descuento aplicado e IVA incluido. Esta consulta no trae la imagen.
 */
public record ProductoCatalogo(
        Long id,
        String codigo,
        String nombre,
        boolean activo,
        int precio,
        int descuento,
        int precioFinal,
        Long categoriaId,
        List<VarianteCatalogo> variantes) {

    public ProductoCatalogo {
        variantes = variantes == null ? List.of() : List.copyOf(variantes);
    }
}
