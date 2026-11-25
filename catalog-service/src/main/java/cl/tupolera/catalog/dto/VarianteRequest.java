package cl.tupolera.catalog.dto;

import java.math.BigDecimal;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Stock y medidas de una talla. Las claves de medidas son los ids definidos en la categoría. */
public record VarianteRequest(
        @Schema(example = "10")
        @NotNull(message = "El stock de cada talla es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo") Integer stock,

        @Schema(example = "{\"contorno_pecho\": 102, \"ancho_hombros\": 44, \"largo_prenda\": 68}")
        Map<String, BigDecimal> medidas) {
}
