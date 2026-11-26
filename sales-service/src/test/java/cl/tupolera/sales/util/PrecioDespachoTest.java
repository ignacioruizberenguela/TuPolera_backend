package cl.tupolera.sales.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Los valores esperados se obtuvieron ejecutando la función calcularPrecioDespacho de despacho.js (frontend)
 * con la misma configuración, para comprobar que el backend reproduce exactamente su comportamiento.
 */
class PrecioDespachoTest {

    private static long conConfiguracionInicial(double km) {
        // Precio base 2990, 150 por km, mínimo 2990 y máximo 15000.
        return PrecioDespacho.calcular(km, 2990, 150, 2990L, 15000L);
    }

    @ParameterizedTest(name = "{0} km = ${1}")
    @CsvSource({
            "0, 2990",
            "0.5, 3070",       // 3065 -> 306,5 -> sube a 307 -> 3070 (el .5 sube)
            "1, 3140",
            "5, 3740",
            "10, 4490",
            "12.34, 4840",
            "25, 6740",
            "33.333, 7990",
            "47.1, 10060",
            "99.99, 15000",    // 17989 -> se limita al máximo
            "100, 15000",
            "250, 15000"
    })
    void configuracionInicialCoincideConElFrontend(double km, long esperado) {
        assertThat(conConfiguracionInicial(km)).isEqualTo(esperado);
    }

    @Test
    void distanciaCeroCobraElPrecioBase() {
        assertThat(conConfiguracionInicial(0)).isEqualTo(2990);
    }

    @Test
    void unaDistanciaNegativaSeTrataComoCero() {
        assertThat(conConfiguracionInicial(-5)).isEqualTo(2990);
        assertThat(conConfiguracionInicial(-0.001)).isEqualTo(2990);
    }

    @Test
    void sinTarifasMinimaNiMaximaNoSeLimitaElPrecio() {
        assertThat(PrecioDespacho.calcular(0, 2990, 150, null, null)).isEqualTo(2990);
        assertThat(PrecioDespacho.calcular(10, 2990, 150, null, null)).isEqualTo(4490);
        assertThat(PrecioDespacho.calcular(100, 2990, 150, null, null)).isEqualTo(17990);
    }

    @Test
    void laTarifaMinimaSeAplicaCuandoElPrecioCalculadoEsMenor() {
        assertThat(PrecioDespacho.calcular(5, 1000, 10, 3000L, null)).isEqualTo(3000);
        assertThat(PrecioDespacho.calcular(500, 1000, 10, 3000L, null)).isEqualTo(6000);   // sobre el mínimo no cambia
    }

    @Test
    void laTarifaMaximaSeAplicaCuandoElPrecioCalculadoEsMayor() {
        assertThat(PrecioDespacho.calcular(100, 2990, 150, null, 8000L)).isEqualTo(8000);
        assertThat(PrecioDespacho.calcular(1, 2990, 150, null, 8000L)).isEqualTo(3140);    // bajo el máximo no cambia
    }

    @Test
    void elRedondeoALaDecenaMasCercanaSubeLosCincos() {
        assertThat(PrecioDespacho.calcular(0, 2995, 0, null, null)).isEqualTo(3000);   // 299,5 -> 300
        assertThat(PrecioDespacho.calcular(0, 2994, 0, null, null)).isEqualTo(2990);   // 299,4 -> 299
        assertThat(PrecioDespacho.calcular(0, 2996, 0, null, null)).isEqualTo(3000);   // 299,6 -> 300
    }

    @Test
    void elResultadoSiempreEsMultiploDeDiez() {
        for (double km = 0; km <= 300; km += 0.37) {
            assertThat(conConfiguracionInicial(km) % 10).isZero();
        }
    }
}
