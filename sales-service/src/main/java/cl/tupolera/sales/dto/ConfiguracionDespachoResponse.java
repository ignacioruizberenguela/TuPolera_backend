package cl.tupolera.sales.dto;

import java.time.Instant;

public record ConfiguracionDespachoResponse(
        String nombreBodega,
        String direccionBodega,
        double latitudBodega,
        double longitudBodega,
        long precioBase,
        long costoPorKm,
        Long tarifaMinima,
        Long tarifaMaxima,
        boolean esUbicacionPrueba,
        Instant fechaActualizacion) {
}
