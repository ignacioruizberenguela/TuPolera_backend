package cl.tupolera.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "juan@gmail.com")
        @NotBlank(message = "El correo es obligatorio") String correo,

        @NotBlank(message = "La contraseña es obligatoria") String password) {
}
