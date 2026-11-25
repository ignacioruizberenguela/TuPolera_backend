package cl.tupolera.catalog.dto;

import java.util.List;

/**
 * Datos del catálogo que sales-service necesita para validar una compra. "precioFinal" es el precio de venta
 * vigente en pesos chilenos: ya tiene el descuento aplicado (misma fórmula que el catálogo y el frontend)
 * y ya incluye el IVA del 19%, que sales-service desglosa sin modificar el valor.
 */
public record ProductoInventarioResponse(
        Long id,
        String codigo,
        String nombre,
        boolean activo,
        int precio,
        int descuento,
        int precioFinal,
        Long categoriaId,
        List<VarianteInventarioResponse> variantes) {
}
