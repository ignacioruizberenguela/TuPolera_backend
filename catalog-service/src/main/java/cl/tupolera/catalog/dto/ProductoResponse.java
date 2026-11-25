package cl.tupolera.catalog.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Respuesta de producto con la misma forma que el frontend ya usa
 * (id, nombre, categoriaId, precio, color, descuento, tallas, atributos, descripcion, imagen),
 * más los campos del backend (codigo, stock total, stock crítico, activo y fechas).
 */
public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        int precio,
        int descuento,
        int precioFinal,
        String color,
        Long categoriaId,
        String categoria,
        String imagen,
        Map<String, VarianteResponse> tallas,
        Map<String, Object> atributos,
        int stock,
        int stockCritico,
        boolean critico,
        List<String> tallasCriticas,
        boolean activo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion) {
}
