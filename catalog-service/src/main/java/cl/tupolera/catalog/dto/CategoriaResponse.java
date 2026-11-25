package cl.tupolera.catalog.dto;

import java.util.List;

public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion,
        String imagen,
        boolean activa,
        List<String> tallas,
        List<AtributoDto> atributos,
        List<MedidaDto> medidas) {
}
