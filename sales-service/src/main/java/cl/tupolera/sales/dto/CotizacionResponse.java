package cl.tupolera.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado de una cotización de despacho. La distancia es en línea recta (Haversine), no por calles.
 * "precio" está en pesos chilenos.
 */
public record CotizacionResponse(
        String direccion,
        double latitud,
        double longitud,
        @Schema(description = "Distancia geográfica en línea recta, en km (3 decimales)") double distanciaKm,
        @Schema(example = "HAVERSINE_LINEA_RECTA") String tipoCalculo,
        @Schema(description = "Precio del despacho en CLP") long precio,
        OrigenResponse origen) {

    /** Único tipo de cálculo disponible en esta versión. */
    public static final String HAVERSINE = "HAVERSINE_LINEA_RECTA";
}
