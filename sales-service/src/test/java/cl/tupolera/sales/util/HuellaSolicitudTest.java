package cl.tupolera.sales.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import cl.tupolera.sales.dto.CompraRequest;
import cl.tupolera.sales.dto.DestinoRequest;
import cl.tupolera.sales.dto.ItemCompraRequest;
import cl.tupolera.sales.model.ResultadoPago;

class HuellaSolicitudTest {

    private static final DestinoRequest DESTINO = new DestinoRequest("Av. Providencia 1234", -33.4251, -70.6167);

    private static CompraRequest compra(String clave, List<ItemCompraRequest> items, DestinoRequest destino,
            ResultadoPago pago) {
        return new CompraRequest(clave, items, destino, pago);
    }

    private static final List<ItemCompraRequest> ITEMS = List.of(
            new ItemCompraRequest(1L, "M", 2), new ItemCompraRequest(2L, "L", 1));

    @Test
    void esUnSha256Hexadecimal() {
        String h = HuellaSolicitud.calcular(compra("clave-0001", ITEMS, DESTINO, ResultadoPago.APROBADO));

        assertThat(h).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void esEstableYNoDependeDeLaClaveNiDelOrden() {
        String a = HuellaSolicitud.calcular(compra("clave-0001", ITEMS, DESTINO, ResultadoPago.APROBADO));
        String b = HuellaSolicitud.calcular(compra("otra-clave-9", List.of(ITEMS.get(1), ITEMS.get(0)),
                DESTINO, ResultadoPago.APROBADO));

        assertThat(a).isEqualTo(b);
    }

    @Test
    void cualquierCambioDeContenidoCambiaLaHuella() {
        String base = HuellaSolicitud.calcular(compra("clave-0001", ITEMS, DESTINO, ResultadoPago.APROBADO));

        assertThat(HuellaSolicitud.calcular(compra("clave-0001", ITEMS, DESTINO, ResultadoPago.RECHAZADO)))
                .isNotEqualTo(base);
        assertThat(HuellaSolicitud.calcular(compra("clave-0001",
                List.of(new ItemCompraRequest(1L, "M", 3), ITEMS.get(1)), DESTINO, ResultadoPago.APROBADO)))
                .isNotEqualTo(base);
        assertThat(HuellaSolicitud.calcular(compra("clave-0001", ITEMS,
                new DestinoRequest("Av. Providencia 1234", -33.4252, -70.6167), ResultadoPago.APROBADO)))
                .isNotEqualTo(base);
        assertThat(HuellaSolicitud.calcular(compra("clave-0001", ITEMS,
                new DestinoRequest("Otra calle 55", -33.4251, -70.6167), ResultadoPago.APROBADO)))
                .isNotEqualTo(base);
    }
}
