package cl.tupolera.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Modificación de la configuración de despacho (solo ADMINISTRADOR). Los montos son enteros en CLP. */
public record ConfiguracionDespachoRequest(
        @Schema(example = "Bodega Tu Polera")
        @NotBlank(message = "El nombre de la bodega es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres") String nombreBodega,

        @Schema(example = "Av. Libertador Bernardo O'Higgins 1000, Santiago")
        @NotBlank(message = "La dirección de la bodega es obligatoria")
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres") String direccionBodega,

        @Schema(example = "-33.4372")
        @NotNull(message = "La latitud de la bodega es obligatoria")
        @DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90") Double latitudBodega,

        @Schema(example = "-70.6506")
        @NotNull(message = "La longitud de la bodega es obligatoria")
        @DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180") Double longitudBodega,

        @Schema(example = "2990")
        @NotNull(message = "El precio base es obligatorio")
        @Min(value = 0, message = "El precio base no puede ser negativo")
        @Max(value = 10_000_000, message = "El precio base es demasiado alto") Long precioBase,

        @Schema(example = "150", description = "Pesos adicionales por cada kilómetro")
        @NotNull(message = "El costo por kilómetro es obligatorio")
        @Min(value = 0, message = "El costo por kilómetro no puede ser negativo")
        @Max(value = 1_000_000, message = "El costo por kilómetro es demasiado alto") Long costoPorKm,

        @Schema(example = "2990", description = "Opcional. Si se omite no hay tarifa mínima")
        @Min(value = 0, message = "La tarifa mínima no puede ser negativa")
        @Max(value = 10_000_000, message = "La tarifa mínima es demasiado alta") Long tarifaMinima,

        @Schema(example = "15000", description = "Opcional. Si se omite no hay tarifa máxima")
        @Min(value = 0, message = "La tarifa máxima no puede ser negativa")
        @Max(value = 10_000_000, message = "La tarifa máxima es demasiado alta") Long tarifaMaxima,

        @Schema(description = "Verdadero mientras la dirección de la bodega sea ficticia")
        @NotNull(message = "Debes indicar si es una ubicación de prueba") Boolean esUbicacionPrueba) {
}
