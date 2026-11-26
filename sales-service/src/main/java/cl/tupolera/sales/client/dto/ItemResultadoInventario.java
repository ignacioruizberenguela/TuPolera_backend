package cl.tupolera.sales.client.dto;

public record ItemResultadoInventario(Long productoId, Long varianteId, String talla, int cantidad, int stockActual) {
}
