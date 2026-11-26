package cl.tupolera.sales.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import cl.tupolera.sales.exception.ApiException;

class ResolutorTallasTest {

    @Test
    void coincidenciaExactaTienePrioridad() {
        // "m" y "M" existen a la vez: pedir "m" debe devolver exactamente "m", sin ambigüedad.
        assertThat(ResolutorTallas.resolver("m", List.of("S", "m", "M"))).isEqualTo("m");
        assertThat(ResolutorTallas.resolver("M", List.of("S", "m", "M"))).isEqualTo("M");
    }

    @Test
    void sinExactaUsaLaUnicaCoincidenciaSinDistinguirMayusculas() {
        assertThat(ResolutorTallas.resolver("xl", List.of("S", "M", "XL"))).isEqualTo("XL");
        assertThat(ResolutorTallas.resolver(" xl ", List.of("S", "M", "XL"))).isEqualTo("XL");
    }

    @Test
    void variasCoincidenciasPosiblesDan400() {
        assertThatThrownBy(() -> ResolutorTallas.resolver("xl", List.of("Xl", "xL")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getMessage()).contains("ambigua");
                });
    }

    @Test
    void tallaInexistenteDa400() {
        assertThatThrownBy(() -> ResolutorTallas.resolver("XXL", List.of("S", "M")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void tallaVaciaDa400() {
        assertThatThrownBy(() -> ResolutorTallas.resolver("  ", List.of("S")))
                .isInstanceOf(ApiException.class);
    }
}
