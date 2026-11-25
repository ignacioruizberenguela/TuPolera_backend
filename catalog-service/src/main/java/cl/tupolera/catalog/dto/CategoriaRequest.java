package cl.tupolera.catalog.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @Schema(example = "Pantalones")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres") String nombre,

        @Schema(example = "Pantalones básicos de uso diario.")
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres") String descripcion,

        @Schema(example = "imagen1", description = "Nombre de una imagen del frontend o una imagen subida (data URL)")
        @Size(max = 3_000_000, message = "La imagen es demasiado grande") String imagen,

        @Schema(description = "Si se omite, la categoría se crea activa") Boolean activa,

        @Schema(example = "[\"38\", \"40\", \"42\"]", description = "Tallas disponibles (cualquier sistema: S, M; 38, 40; Única)")
        @NotEmpty(message = "La categoría debe tener al menos una talla")
        List<@NotBlank(message = "Las tallas no pueden estar vacías")
                @Size(max = 30, message = "Una talla no puede superar los 30 caracteres") String> tallas,

        List<@Valid AtributoDto> atributos,

        List<@Valid MedidaDto> medidas) {
}
