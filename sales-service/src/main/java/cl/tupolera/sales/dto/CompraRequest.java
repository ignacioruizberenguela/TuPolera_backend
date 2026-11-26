package cl.tupolera.sales.dto;

import java.util.List;

import cl.tupolera.sales.model.ResultadoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de compra. No incluye precios, IVA, distancia ni totales: el backend los obtiene del catálogo y de
 * la configuración oficial de despacho. Reenviar la misma solicitud con la misma clave retoma la compra.
 */
public record CompraRequest(
        @Schema(example = "3f2b8c9e-6a41-4d52-9c1e-0a7b5d2e8f10",
                description = "Una clave nueva por compra (se recomienda un UUID). Misma clave + mismo contenido = misma compra.")
        @NotNull(message = "La clave de idempotencia es obligatoria")
        @Pattern(regexp = "^[A-Za-z0-9._-]{8,64}$",
                message = "La clave debe tener entre 8 y 64 caracteres (letras, números, punto, guion o guion bajo)")
        String claveIdempotencia,

        @NotEmpty(message = "Debes indicar al menos un producto")
        @Size(max = 100, message = "Una compra admite hasta 100 líneas")
        List<@Valid @NotNull(message = "Los ítems no pueden ser nulos") ItemCompraRequest> items,

        @NotNull(message = "El destino es obligatorio") @Valid DestinoRequest destino,

        @Schema(description = "Resultado de la simulación de pago elegido por el cliente")
        @NotNull(message = "El resultado del pago es obligatorio (APROBADO o RECHAZADO)")
        ResultadoPago resultadoPago) {
}
