package cl.tupolera.sales.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Locale;
import java.util.stream.Collectors;

import cl.tupolera.sales.dto.CompraRequest;
import cl.tupolera.sales.dto.ItemCompraRequest;

/**
 * Resumen SHA-256 (64 caracteres hexadecimales) del contenido de una compra. Sirve para la idempotencia:
 * la misma clave con el mismo contenido retoma la compra; con contenido distinto es un conflicto (409).
 * No depende del orden de los ítems. La clave de idempotencia no forma parte del resumen.
 */
public final class HuellaSolicitud {

    private HuellaSolicitud() {
    }

    public static String calcular(CompraRequest r) {
        String items = r.items().stream()
                .sorted(Comparator.comparing(ItemCompraRequest::productoId)
                        .thenComparing(i -> i.talla().strip())
                        .thenComparing(ItemCompraRequest::cantidad))
                .map(i -> i.productoId() + ":" + i.talla().strip() + ":" + i.cantidad())
                .collect(Collectors.joining(","));

        String canonico = String.join("\n",
                "items=" + items,
                "pago=" + r.resultadoPago(),
                "dir=" + r.destino().direccion().strip(),
                "lat=" + String.format(Locale.ROOT, "%.7f", r.destino().latitud()),
                "lng=" + String.format(Locale.ROOT, "%.7f", r.destino().longitud()));

        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonico.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
