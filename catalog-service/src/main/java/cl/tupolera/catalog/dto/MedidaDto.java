package cl.tupolera.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Medida configurable de una categoría (mismos nombres de campo que usa el frontend). */
public record MedidaDto(
        @Schema(example = "contorno_pecho", description = "Identificador estable. Si se omite se genera desde el nombre.")
        @Pattern(regexp = "^$|^[a-z0-9_]{1,60}$", message = "El id de la medida solo admite minúsculas, números y guion bajo")
        String id,

        @Schema(example = "Contorno de pecho")
        @NotBlank(message = "Todas las medidas deben tener un nombre")
        @Size(max = 80, message = "El nombre de la medida no puede superar los 80 caracteres") String nombre,

        @Schema(example = "cm")
        @Size(max = 20, message = "La unidad no puede superar los 20 caracteres") String unidad,

        @Schema(description = "El cliente ingresa esta medida en el comparador de medidas")
        boolean solicitarCliente,

        @Schema(description = "Se registra para cada talla de cada producto")
        boolean registrarProducto) {
}
