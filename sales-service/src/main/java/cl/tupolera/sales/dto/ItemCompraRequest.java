package cl.tupolera.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Un producto del carrito. Solo se recibe qué y cuánto: precios y totales los calcula el backend. */
public record ItemCompraRequest(
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
