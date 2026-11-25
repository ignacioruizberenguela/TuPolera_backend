package cl.tupolera.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Una variante (producto + talla) y la cantidad a descontar o reponer. */
public record ItemInventarioRequest(
        @Schema(example = "1")
        @NotNull(message = "El productoId es obligatorio") Long productoId,

        @Schema(example = "M")
        @NotBlank(message = "La talla es obligatoria")
        @Size(max = 30, message = "La talla no puede superar los 30 caracteres") String talla,

        @Schema(example = "2")
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 10000, message = "La cantidad no puede superar 10000") Integer cantidad) {
}
