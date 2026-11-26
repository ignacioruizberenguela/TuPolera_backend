package cl.tupolera.sales.client.dto;

/** Solo los campos de GET /api/v1/productos/{id} que sales-service necesita (el resto se ignora). */
public record ProductoImagen(Long id, String imagen) {
}
