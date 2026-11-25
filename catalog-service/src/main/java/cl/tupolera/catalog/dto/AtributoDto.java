package cl.tupolera.catalog.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Atributo configurable de una categoría (mismos nombres de campo que usa el frontend). */
public record AtributoDto(
        @Schema(example = "tipo_manga", description = "Identificador estable. Si se omite se genera desde el nombre.")
        @Pattern(regexp = "^$|^[a-z0-9_]{1,60}$", message = "El id del atributo solo admite minúsculas, números y guion bajo")
        String id,

        @Schema(example = "Tipo de manga")
        @NotBlank(message = "Todos los atributos deben tener un nombre")
        @Size(max = 80, message = "El nombre del atributo no puede superar los 80 caracteres") String nombre,

        @Schema(example = "seleccion", allowableValues = { "texto", "numero", "seleccion", "booleano" })
        @NotBlank(message = "El tipo del atributo es obligatorio")
        @Pattern(regexp = "texto|numero|seleccion|booleano",
                message = "El tipo debe ser texto, numero, seleccion o booleano") String tipo,

        @Schema(example = "[\"Corta\", \"Larga\"]", description = "Obligatorio (mínimo 2) cuando el tipo es seleccion")
        List<@NotBlank(message = "Las opciones no pueden estar vacías")
                @Size(max = 80, message = "Una opción no puede superar los 80 caracteres") String> opciones,

        boolean obligatorio) {
}
