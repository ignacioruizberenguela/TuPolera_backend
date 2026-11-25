package cl.tupolera.auth.dto;

import java.time.LocalDate;

import cl.tupolera.auth.validation.RunChileno;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/** Datos del registro público. El rol siempre será CLIENTE; no se puede enviar. */
public record RegistroRequest(
        @Schema(example = "12.345.678-5", description = "RUN chileno; se guarda sin puntos ni guion")
        @NotBlank(message = "El RUN es obligatorio") @RunChileno String run,

        @Schema(example = "Juan")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres") String nombre,

        @Schema(example = "Pérez Soto")
        @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres") String apellidos,

        @Schema(example = "juan@gmail.com")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo no puede superar los 120 caracteres") String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres") String password,

        @Past(message = "La fecha de nacimiento debe ser anterior a hoy") LocalDate fechaNacimiento,

        @Size(max = 200, message = "La dirección no puede superar los 200 caracteres") String direccion,

        @NotNull(message = "La región es obligatoria") Long idRegion,

        @NotNull(message = "La comuna es obligatoria") Long idComuna) {
}
