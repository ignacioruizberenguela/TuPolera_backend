package cl.tupolera.catalog.dto;

import java.util.List;

public record ConsultaInventarioResponse(List<ProductoInventarioResponse> productos, List<Long> noEncontrados) {
}
