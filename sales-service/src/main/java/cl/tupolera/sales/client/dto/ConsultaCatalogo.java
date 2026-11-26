package cl.tupolera.sales.client.dto;

import java.util.List;

/** Respuesta de la consulta de inventario: productos encontrados e ids que no existen. */
public record ConsultaCatalogo(List<ProductoCatalogo> productos, List<Long> noEncontrados) {

    public ConsultaCatalogo {
        productos = productos == null ? List.of() : List.copyOf(productos);
        noEncontrados = noEncontrados == null ? List.of() : List.copyOf(noEncontrados);
    }
}
