package cl.tupolera.sales.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import cl.tupolera.sales.exception.ApiException;
import cl.tupolera.sales.util.FusionItems.Item;

class FusionItemsTest {

    @Test
    void sumaLasCantidadesDelMismoProductoYTalla() {
        List<Item> r = FusionItems.fusionar(List.of(new Item(1L, "M", 2), new Item(2L, "M", 1), new Item(1L, "M", 3)));

        assertThat(r).containsExactly(new Item(1L, "M", 5), new Item(2L, "M", 1));
    }

    @Test
    void mismaTallaEnOtroProductoOOtraTallaNoSeFusiona() {
        List<Item> r = FusionItems.fusionar(List.of(new Item(1L, "M", 1), new Item(1L, "L", 1), new Item(2L, "M", 1)));

        assertThat(r).hasSize(3);
    }

    @Test
    void permiteExactamenteElMaximo() {
        List<Item> r = FusionItems.fusionar(List.of(new Item(1L, "M", 9_000), new Item(1L, "M", 1_000)));

        assertThat(r).containsExactly(new Item(1L, "M", FusionItems.MAXIMO_CANTIDAD));
    }

    @Test
    void siLaSumaSuperaElMaximoDa400() {
        assertThatThrownBy(() -> FusionItems.fusionar(
                List.of(new Item(1L, "M", 9_000), new Item(1L, "M", 1_001))))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void sumaGrandeNoDesbordaElEntero() {
        assertThatThrownBy(() -> FusionItems.fusionar(
                List.of(new Item(1L, "M", Integer.MAX_VALUE), new Item(1L, "M", Integer.MAX_VALUE))))
                .isInstanceOf(ApiException.class);
    }
}
