package cl.tupolera.sales.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DineroTest {

    @ParameterizedTest(name = "precio {0} con {1}% de descuento = {2}")
    @CsvSource({
            "12990, 50, 6495",
            "14990, 10, 13491",
            "24990, 25, 18743",   // 18742,5 se redondea hacia arriba, igual que Math.round en el frontend
            "8990, 0, 8990",
            "100, 100, 0",
            "1, 50, 1"            // 0,5 sube a 1
    })
    void precioFinalUsaLaFormulaDelFrontend(long precio, int descuento, long esperado) {
        assertThat(Dinero.precioFinal(precio, descuento)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "bruto {0} = neto {1} + IVA {2}")
    @CsvSource({
            "6495, 5458, 1037",
            "13491, 11337, 2154",
            "26481, 22253, 4228",
            "11900, 10000, 1900",
            "119, 100, 19",
            "1, 1, 0",
            "0, 0, 0"
    })
    void separaNetoEIvaDeUnMontoBruto(long bruto, long neto, long iva) {
        assertThat(Dinero.neto(bruto)).isEqualTo(neto);
        assertThat(Dinero.iva(bruto)).isEqualTo(iva);
    }

    @Test
    void netoMasIvaSiempreSumanElBruto() {
        for (long bruto = 0; bruto <= 300_000; bruto++) {
            assertThat(Dinero.neto(bruto) + Dinero.iva(bruto)).isEqualTo(bruto);
        }
    }

    @Test
    void laFormulaEnteraEquivaleARedondearBrutoEntre119() {
        // neto = round(bruto / 1,19) con redondeo hacia arriba en el .5, calculado de forma exacta con BigDecimal.
        for (long bruto = 0; bruto <= 300_000; bruto++) {
            long exacto = BigDecimal.valueOf(bruto * 100).divide(BigDecimal.valueOf(119), 0, RoundingMode.HALF_UP)
                    .longValueExact();
            assertThat(Dinero.neto(bruto)).as("bruto %d", bruto).isEqualTo(exacto);
        }
    }

    @Test
    void elIvaSeCalculaSobreElSubtotalDeProductosYNoSobreElDespacho() {
        long subtotalProductos = Dinero.precioFinal(12990, 50) * 2 + Dinero.precioFinal(14990, 10);
        long despacho = 3500;

        assertThat(subtotalProductos).isEqualTo(26481);

        long neto = Dinero.neto(subtotalProductos);
        long iva = Dinero.iva(subtotalProductos);
        long total = subtotalProductos + despacho;

        assertThat(neto).isEqualTo(22253);
        assertThat(iva).isEqualTo(4228);
        assertThat(neto + iva).isEqualTo(subtotalProductos);   // el despacho no entra en el neto ni en el IVA
        assertThat(total).isEqualTo(29981);

        // Si el IVA se calculara (mal) sobre el total con despacho, el resultado sería distinto.
        assertThat(Dinero.iva(total)).isNotEqualTo(iva);
    }
}
