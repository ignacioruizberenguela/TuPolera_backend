package cl.tupolera.sales.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PoliticaImagenTest {

    @Test
    void sinImagenNoGuardaNada() {
        assertThat(PoliticaImagen.aplicar(null)).isEmpty();
        assertThat(PoliticaImagen.aplicar("   ")).isEmpty();
    }

    @Test
    void imagenPequenaSeConserva() {
        assertThat(PoliticaImagen.aplicar("polera-negra.png")).contains("polera-negra.png");
    }

    @Test
    void imagenDeExactamenteUnMegaSeConserva() {
        String exacta = "a".repeat(PoliticaImagen.MAXIMO_BYTES);
        assertThat(PoliticaImagen.aplicar(exacta)).isPresent();
    }

    @Test
    void imagenQueSuperaUnMegaNoSeGuarda() {
        String grande = "a".repeat(PoliticaImagen.MAXIMO_BYTES + 1);
        assertThat(PoliticaImagen.aplicar(grande)).isEmpty();
    }

    @Test
    void elTamanoSeMideEnBytesNoEnCaracteres() {
        // "ñ" ocupa 2 bytes en UTF-8: 600.000 caracteres superan 1 MB aunque tengan menos caracteres que el límite.
        String conTildes = "ñ".repeat(600_000);
        assertThat(PoliticaImagen.aplicar(conTildes)).isEmpty();
    }
}
