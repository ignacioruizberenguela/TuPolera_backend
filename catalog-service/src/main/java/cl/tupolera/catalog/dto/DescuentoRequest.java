package cl.tupolera.catalog.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Descuento atómico de varias variantes. La "referencia" la genera sales-service ANTES de pedir el descuento
 * (idealmente un UUID) e identifica la operación: repetirla no vuelve a descontar, y la misma referencia
 * permite revertir el descuento.
 */
public record DescuentoRequest(
        @Schema(example = "3f2b8c9e-6a41-4d52-9c1e-0a7b5d2e8f10")
        @NotBlank(message = "La referencia es obligatoria")
        @Pattern(regexp = "^[A-Za-z0-9._-]{8,64}$",
                message = "La referencia debe tener entre 8 y 64 caracteres (letras, números, punto, guion o guion bajo); se recomienda un UUID")
        String referencia,

        @NotEmpty(message = "Debes indicar al menos un ítem")
        @Size(max = 100, message = "Se pueden descontar hasta 100 ítems por solicitud")
        List<@Valid @NotNull(message = "Los ítems no pueden ser nulos") ItemInventarioRequest> items) {
}
