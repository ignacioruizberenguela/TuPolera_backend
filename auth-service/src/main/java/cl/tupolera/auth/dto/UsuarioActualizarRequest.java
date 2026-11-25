package cl.tupolera.auth.dto;

import java.time.LocalDate;

import cl.tupolera.auth.model.Rol;
import cl.tupolera.auth.validation.RunChileno;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/** Actualización por un administrador. Si la contraseña viene vacía, se conserva la actual. */
public record UsuarioActualizarRequest(
        @NotBlank(message = "El RUN es obligatorio") @RunChileno String run,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres") String nombre,

        @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres") String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo no puede superar los 120 caracteres") String correo,

        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres") String password,

        @Past(message = "La fecha de nacimiento debe ser anterior a hoy") LocalDate fechaNacimiento,

        @Size(max = 200, message = "La dirección no puede superar los 200 caracteres") String direccion,

        @NotNull(message = "La región es obligatoria") Long idRegion,

        @NotNull(message = "La comuna es obligatoria") Long idComuna,

        @NotNull(message = "El rol es obligatorio") Rol rol,

        @NotNull(message = "El estado activo es obligatorio") Boolean activo) {
}
