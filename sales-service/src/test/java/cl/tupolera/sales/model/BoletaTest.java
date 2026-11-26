package cl.tupolera.sales.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BoletaTest {

    private static Boleta nueva() {
        return new Boleta(7L, "TP-abc", "clave-0001", "h".repeat(64), ResultadoPago.APROBADO);
    }

    @Test
    void naceComoPendienteConInventarioPorConfirmar() {
        Boleta b = nueva();

        assertThat(b.getEstado()).isEqualTo(EstadoBoleta.PENDIENTE);
        assertThat(b.getEstadoInventario()).isEqualTo(EstadoInventario.PENDIENTE);
    }

    @Test
    void asignaElNumeroDesdeElIdUnaSolaVez() {
        Boleta b = nueva();
        assertThat(b.getNumeroBoleta()).isNull();

        b.asignarNumero(); // aún sin id: no hace nada
        assertThat(b.getNumeroBoleta()).isNull();

        ReflectionTestUtils.setField(b, "id", 12L);
        b.asignarNumero();
        assertThat(b.getNumeroBoleta()).isEqualTo("BOL-0012");

        ReflectionTestUtils.setField(b, "id", 99L);
        b.asignarNumero();
        assertThat(b.getNumeroBoleta()).isEqualTo("BOL-0012");
    }

    @Test
    void detalleCalculaSuSubtotalYManejaLaImagen() {
        BoletaDetalle d = new BoletaDetalle(1L, "P001", "Polera", "M", 10_000, 10, 9_000, 3);

        assertThat(d.getSubtotal()).isEqualTo(27_000);
        assertThat(d.isImagenDisponible()).isFalse();

        d.asignarImagen("polera.png");
        assertThat(d.isImagenDisponible()).isTrue();
        assertThat(d.getImagen()).isEqualTo("polera.png");

        d.asignarImagen(null);
        assertThat(d.isImagenDisponible()).isFalse();
        assertThat(d.getImagen()).isNull();
    }

    @Test
    void agregarDetalleLoVinculaConSuBoleta() {
        Boleta b = nueva();
        BoletaDetalle d = new BoletaDetalle(1L, "P001", "Polera", "M", 10_000, 0, 10_000, 1);

        b.agregarDetalle(d);

        assertThat(b.getDetalles()).containsExactly(d);
        assertThat(d.getBoleta()).isSameAs(b);
    }

    @Test
    void elDetalleDelEventoSeRecortaAlLimiteDeLaColumna() {
        BoletaEvento e = new BoletaEvento(1L, BoletaEvento.DESCUENTO_INTENTO, "x".repeat(900));

        assertThat(e.getDetalle()).hasSize(500);
    }

    @Test
    void noSePuedeAprobarSinInventarioDescontado() {
        for (EstadoInventario inv : EstadoInventario.values()) {
            if (inv == EstadoInventario.DESCONTADO) continue;
            Boleta b = nueva();
            assertThatThrownBy(() -> b.cambiarEstado(EstadoBoleta.APROBADA, inv, null))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(b.getEstado()).isEqualTo(EstadoBoleta.PENDIENTE);
        }
    }

    @Test
    void apruebaConInventarioDescontado() {
        Boleta b = nueva();

        b.cambiarEstado(EstadoBoleta.APROBADA, EstadoInventario.DESCONTADO, null);

        assertThat(b.getEstado()).isEqualTo(EstadoBoleta.APROBADA);
        assertThat(b.getEstadoInventario()).isEqualTo(EstadoInventario.DESCONTADO);
    }
}
