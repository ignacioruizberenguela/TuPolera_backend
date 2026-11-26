package cl.tupolera.sales.util;

/**
 * Utilidades geográficas.
 *
 * Haversine calcula la distancia GEOGRÁFICA EN LÍNEA RECTA (sobre la esfera terrestre) entre dos puntos.
 * NO es la distancia real por calles: el recorrido en auto suele ser mayor. Es una aproximación suficiente
 * para la primera versión del cálculo de despacho (en una etapa futura se puede reemplazar por una ruta real).
 */
public final class Geo {

    /** Radio medio de la Tierra en km (mismo valor que usa el frontend). */
    private static final double RADIO_TIERRA_KM = 6371.0;

    // Rectángulo que contiene a Chile continental, Isla de Pascua y Juan Fernández de forma aproximada.
    // Es el mismo que usa el frontend. Es una validación aproximada, no un polígono exacto del país.
    public static final double LATITUD_MIN_CHILE = -56.5;
    public static final double LATITUD_MAX_CHILE = -17.4;
    public static final double LONGITUD_MIN_CHILE = -110.0;
    public static final double LONGITUD_MAX_CHILE = -66.0;

    private Geo() {
    }

    /** Distancia en kilómetros entre dos puntos (fórmula de Haversine). */
    public static double distanciaKm(double latitud1, double longitud1, double latitud2, double longitud2) {
        double dLatitud = aRadianes(latitud2 - latitud1);
        double dLongitud = aRadianes(longitud2 - longitud1);

        double seno1 = Math.sin(dLatitud / 2);
        double seno2 = Math.sin(dLongitud / 2);

        double a = seno1 * seno1
                + Math.cos(aRadianes(latitud1)) * Math.cos(aRadianes(latitud2)) * seno2 * seno2;

        return RADIO_TIERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Latitud entre -90 y 90 y longitud entre -180 y 180, y que sean números finitos (no NaN ni infinito). */
    public static boolean esCoordenadaValida(double latitud, double longitud) {
        return Double.isFinite(latitud) && Double.isFinite(longitud)
                && latitud >= -90 && latitud <= 90
                && longitud >= -180 && longitud <= 180;
    }

    /** Coordenada válida y dentro del rectángulo aproximado de Chile. */
    public static boolean estaEnChile(double latitud, double longitud) {
        return esCoordenadaValida(latitud, longitud)
                && latitud >= LATITUD_MIN_CHILE && latitud <= LATITUD_MAX_CHILE
                && longitud >= LONGITUD_MIN_CHILE && longitud <= LONGITUD_MAX_CHILE;
    }

    private static double aRadianes(double grados) {
        return grados * Math.PI / 180;
    }
}
