package cl.tupolera.auth.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import cl.tupolera.auth.model.Rol;

/** Datos públicos del usuario. Nunca incluye la contraseña. */
public record UsuarioResponse(
        Long id,
        String run,
        String nombre,
        String apellidos,
        String correo,
        LocalDate fechaNacimiento,
        String direccion,
        Long idRegion,
        String region,
        Long idComuna,
        String comuna,
        Rol rol,
        boolean activo,
        LocalDateTime fechaCreacion) {
}
