package cl.tupolera.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Destino del despacho elegido por el cliente en el mapa de Google Maps del frontend.
 * El frontend envía la dirección legible (geocodificación inversa) y las coordenadas del punto seleccionado.
 * La distancia NUNCA se recibe: la calcula el backend.
 */
@Schema(description = "Destino elegido en el mapa: dirección legible y coordenadas del punto seleccionado")
public record DestinoRequest(
        @Schema(example = "Av. Providencia 1234, Providencia, Santiago")
        @NotBlank(message = "La dirección de despacho es obligatoria")
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres") String direccion,

        @Schema(example = "-33.4251", description = "Latitud en grados decimales (-90 a 90)")
        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90") Double latitud,

        @Schema(example = "-70.6167", description = "Longitud en grados decimales (-180 a 180)")
        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180") Double longitud) {
}
