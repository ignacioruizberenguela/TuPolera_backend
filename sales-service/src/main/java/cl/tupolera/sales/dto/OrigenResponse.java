package cl.tupolera.sales.dto;

/** Bodega desde la que salen los despachos. */
public record OrigenResponse(String nombre, String direccion, double latitud, double longitud,
        boolean esUbicacionPrueba) {
}
