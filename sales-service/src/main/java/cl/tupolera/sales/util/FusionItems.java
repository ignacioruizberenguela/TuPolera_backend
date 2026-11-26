package cl.tupolera.sales.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cl.tupolera.sales.exception.ApiException;

/**
 * Decisión aprobada: si el mismo producto y talla llegan repetidos, se fusionan sumando las cantidades
 * (la tabla boleta_detalle solo admite una fila por boleta, producto y talla). Se aplica DESPUÉS de resolver
 * las tallas, para que "m" y "M" (misma talla del catálogo) también se fusionen.
 */
public final class FusionItems {

    /** Máximo de unidades por línea; es el mismo límite que usa catalog-service por ítem. */
    public static final int MAXIMO_CANTIDAD = 10_000;

    private FusionItems() {
    }

    public record Item(Long productoId, String talla, int cantidad) {
    }

    /** Conserva el orden de primera aparición. Si una suma supera el máximo, responde 400. */
    public static List<Item> fusionar(List<Item> items) {
        Map<String, Item> porClave = new LinkedHashMap<>();

        for (Item item : items) {
            String clave = item.productoId() + "|" + item.talla();
            Item existente = porClave.get(clave);
            long suma = (existente == null ? 0L : existente.cantidad()) + (long) item.cantidad();

            if (suma > MAXIMO_CANTIDAD) {
                throw ApiException.badRequest("La cantidad total de " + item.talla() + " del producto "
                        + item.productoId() + " supera el máximo de " + MAXIMO_CANTIDAD + " unidades.");
            }
            porClave.put(clave, new Item(item.productoId(), item.talla(), (int) suma));
        }
        return new ArrayList<>(porClave.values());
    }
}
