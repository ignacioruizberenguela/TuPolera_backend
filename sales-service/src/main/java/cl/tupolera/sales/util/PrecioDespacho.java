package cl.tupolera.sales.util;

/**
 * Fórmula del precio de despacho. Reproduce la de despacho.js del frontend:
 *
 *   precio = precioBase + costoPorKm * max(0, distanciaKm)
 *   luego se aplica la tarifa mínima y la máxima (si están configuradas)
 *   y se redondea a la decena de pesos más cercana con Math.round (los .5 suben).
 *
 * Se usa aritmética double igual que JavaScript para obtener exactamente los mismos resultados.
 */
public final class PrecioDespacho {

    private PrecioDespacho() {
    }

    /**
     * @param tarifaMinima tarifa mínima, o null si no hay
     * @param tarifaMaxima tarifa máxima, o null si no hay
     */
    public static long calcular(double distanciaKm, long precioBase, long costoPorKm, Long tarifaMinima,
            Long tarifaMaxima) {

        double precio = precioBase + costoPorKm * Math.max(0, distanciaKm);

        if (tarifaMinima != null) {
            precio = Math.max(precio, tarifaMinima);
        }

        if (tarifaMaxima != null) {
            precio = Math.min(precio, tarifaMaxima);
        }

        return Math.round(precio / 10) * 10;
    }
}
