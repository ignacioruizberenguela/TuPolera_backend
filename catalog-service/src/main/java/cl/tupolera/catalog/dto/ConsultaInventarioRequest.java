package cl.tupolera.catalog.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConsultaInventarioRequest(
        @NotEmpty(message = "Debes indicar al menos un producto")
        @Size(max = 100, message = "Se pueden consultar hasta 100 productos por solicitud")
        List<@NotNull(message = "Los ids de producto no pueden ser nulos") Long> productoIds) {
}
