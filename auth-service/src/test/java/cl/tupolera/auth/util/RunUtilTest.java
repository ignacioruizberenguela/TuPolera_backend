package cl.tupolera.auth.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RunUtilTest {

    @Test
    void normalizaPuntosGuionYMayusculas() {
        assertThat(RunUtil.normalizar("12.345.678-5")).isEqualTo("123456785");
        assertThat(RunUtil.normalizar(" 7.967.980-1 ")).isEqualTo("79679801");
        assertThat(RunUtil.normalizar("11111111-k")).isEqualTo("11111111K");
    }

    @Test
    void aceptaRunConDigitoVerificadorCorrecto() {
        assertThat(RunUtil.esValido("12.345.678-5")).isTrue();
        assertThat(RunUtil.esValido("3632297-7")).isTrue();
        assertThat(RunUtil.esValido("34798536-8")).isTrue();
        assertThat(RunUtil.esValido("7967980-1")).isTrue();
    }

    @Test
    void rechazaDigitoVerificadorIncorrectoOFormatoInvalido() {
        assertThat(RunUtil.esValido("12.345.678-9")).isFalse();
        assertThat(RunUtil.esValido("abc")).isFalse();
        assertThat(RunUtil.esValido("123")).isFalse();
        assertThat(RunUtil.esValido(null)).isFalse();
    }
}
