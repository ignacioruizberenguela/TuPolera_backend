package cl.tupolera.sales.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoTest {

    // Bodega de prueba (Santiago centro).
    private static final double ORIGEN_LAT = -33.4372;
    private static final double ORIGEN_LON = -70.6506;

    @Test
    void distanciaCeroEntreUnPuntoYSiMismo() {
        assertThat(Geo.distanciaKm(ORIGEN_LAT, ORIGEN_LON, ORIGEN_LAT, ORIGEN_LON)).isZero();
    }

    @Test
    void distanciasCoincidenConLaFormulaDelFrontend() {
        // Valores obtenidos ejecutando la función calcularDistanciaLineaRecta del frontend con los mismos puntos.
        assertThat(Geo.distanciaKm(ORIGEN_LAT, ORIGEN_LON, -33.4251, -70.6167))
                .isCloseTo(3.4214835687982292, within(1e-6));        // Providencia
        assertThat(Geo.distanciaKm(ORIGEN_LAT, ORIGEN_LON, -33.0472, -71.6127))
                .isCloseTo(99.4291450729472, within(1e-6));          // Valparaíso
        assertThat(Geo.distanciaKm(ORIGEN_LAT, ORIGEN_LON, -36.8201, -73.0444))
                .isCloseTo(434.57211215234764, within(1e-6));        // Concepción
        assertThat(Geo.distanciaKm(ORIGEN_LAT, ORIGEN_LON, -41.4693, -72.9424))
                .isCloseTo(915.6515824497469, within(1e-6));         // Puerto Montt
        assertThat(Geo.distanciaKm(ORIGEN_LAT, ORIGEN_LON, -18.4783, -70.3126))
                .isCloseTo(1663.6936654319018, within(1e-6));        // Arica
    }

    @Test
    void laDistanciaEsSimetrica() {
        double ida = Geo.distanciaKm(ORIGEN_LAT, ORIGEN_LON, -36.8201, -73.0444);
        double vuelta = Geo.distanciaKm(-36.8201, -73.0444, ORIGEN_LAT, ORIGEN_LON);

        assertThat(ida).isCloseTo(vuelta, within(1e-9));
    }

    @Test
    void unGradoDeLatitudEquivaleAAproximadamente111Km() {
        assertThat(Geo.distanciaKm(-33, -70, -34, -70)).isCloseTo(111.19, within(0.1));
    }

    @Test
    void coordenadasValidasRequierenNumerosFinitosYRangos() {
        assertThat(Geo.esCoordenadaValida(-33.4, -70.6)).isTrue();
        assertThat(Geo.esCoordenadaValida(90, 180)).isTrue();
        assertThat(Geo.esCoordenadaValida(-90, -180)).isTrue();

        assertThat(Geo.esCoordenadaValida(90.0001, 0)).isFalse();
        assertThat(Geo.esCoordenadaValida(0, 180.0001)).isFalse();
        assertThat(Geo.esCoordenadaValida(Double.NaN, -70)).isFalse();
        assertThat(Geo.esCoordenadaValida(-33, Double.NaN)).isFalse();
        assertThat(Geo.esCoordenadaValida(Double.POSITIVE_INFINITY, -70)).isFalse();
        assertThat(Geo.esCoordenadaValida(-33, Double.NEGATIVE_INFINITY)).isFalse();
    }

    @Test
    void soloSeAceptanDestinosDentroDeChile() {
        assertThat(Geo.estaEnChile(-33.4372, -70.6506)).isTrue();    // Santiago
        assertThat(Geo.estaEnChile(-53.1638, -70.9171)).isTrue();    // Punta Arenas
        assertThat(Geo.estaEnChile(-18.4783, -70.3126)).isTrue();    // Arica
        assertThat(Geo.estaEnChile(-27.1127, -109.3497)).isTrue();   // Isla de Pascua

        assertThat(Geo.estaEnChile(-34.6037, -58.3816)).isFalse();   // Buenos Aires
        assertThat(Geo.estaEnChile(40.4168, -3.7038)).isFalse();     // Madrid
        assertThat(Geo.estaEnChile(0, 0)).isFalse();
        assertThat(Geo.estaEnChile(91, -70)).isFalse();
        assertThat(Geo.estaEnChile(Double.NaN, -70)).isFalse();
    }
}
