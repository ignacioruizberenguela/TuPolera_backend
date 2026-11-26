package cl.tupolera.sales.client.dto;

import java.util.List;

/** "repetida" es true cuando catalog-service ya había aplicado esa referencia (no vuelve a tocar el stock). */
public record ResultadoInventario(String referencia, String tipo, boolean repetida,
        List<ItemResultadoInventario> items) {

    public ResultadoInventario {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
