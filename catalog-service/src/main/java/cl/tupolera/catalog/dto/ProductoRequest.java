package cl.tupolera.catalog.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o actualizar un producto. Usa la misma estructura que el frontend:
 * "tallas" es un objeto talla -> {stock, medidas} y "atributos" un objeto clave -> valor.
 */
public record ProductoRequest(
        @Schema(example = "POL-010", description = "Único, mínimo 3 caracteres")
        @NotBlank(message = "El código es obligatorio")
        @Size(min = 3, max = 30, message = "El código debe tener entre 3 y 30 caracteres") String codigo,

        @Schema(example = "Urban Red")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres") String nombre,

        @Schema(example = "Polera liviana y cómoda.")
        @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres") String descripcion,

        @Schema(example = "12990", description = "Precio en CLP con IVA incluido")
        @NotNull(message = "El precio es obligatorio")
        @Min(value = 0, message = "El precio no puede ser negativo") Integer precio,

        @Schema(example = "50", description = "Descuento en porcentaje (0 a 100). Si se omite, 0")
        @Min(value = 0, message = "El descuento no puede ser menor que 0")
        @Max(value = 100, message = "El descuento no puede ser mayor que 100") Integer descuento,

        @Schema(example = "rojo")
        @Size(max = 40, message = "El color no puede superar los 40 caracteres") String color,

        @Schema(example = "1")
        @NotNull(message = "La categoría es obligatoria") Long categoriaId,

        @Schema(example = "imagen1", description = "Nombre de una imagen del frontend o una imagen subida (data URL)")
        @Size(max = 3_000_000, message = "La imagen es demasiado grande") String imagen,

        @Schema(example = "5", description = "Umbral de stock crítico por talla. Si se omite, 5")
        @Min(value = 0, message = "El stock crítico no puede ser negativo") Integer stockCritico,

        @Schema(description = "Si se omite, el producto se crea activo") Boolean activo,

        @NotEmpty(message = "Debes indicar al menos una talla")
        Map<@NotBlank(message = "El nombre de la talla no puede estar vacío") String, @Valid @NotNull(message = "Cada talla debe indicar su stock") VarianteRequest> tallas,

        @Schema(example = "{\"tipo_manga\": \"Corta\"}")
        Map<String, Object> atributos) {
}
