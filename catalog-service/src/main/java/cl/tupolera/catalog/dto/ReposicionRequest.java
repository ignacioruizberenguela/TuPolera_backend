package cl.tupolera.catalog.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Reposición de stock. La clave de idempotencia evita aplicar dos veces la misma reposición. */
public record ReposicionRequest(
        @Schema(example = "7c1d4a52-91b3-4e7f-8d20-5b6a3c9e1f44")
        @NotBlank(message = "La clave de idempotencia es obligatoria")
        @Pattern(regexp = "^[A-Za-z0-9._-]{8,64}$",
                message = "La clave debe tener entre 8 y 64 caracteres (letras, números, punto, guion o guion bajo); se recomienda un UUID")
        String claveIdempotencia,

        @NotEmpty(message = "Debes indicar al menos un ítem")
        @Size(max = 100, message = "Se pueden reponer hasta 100 ítems por solicitud")
        List<@Valid @NotNull(message = "Los ítems no pueden ser nulos") ItemInventarioRequest> items) {
}
