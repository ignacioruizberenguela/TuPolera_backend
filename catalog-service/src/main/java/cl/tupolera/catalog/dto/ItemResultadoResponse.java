package cl.tupolera.catalog.dto;

public record ItemResultadoResponse(Long productoId, Long varianteId, String talla, int cantidad, int stockActual) {
}
