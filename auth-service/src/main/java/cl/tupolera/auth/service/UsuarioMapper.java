package cl.tupolera.auth.service;

import org.springframework.stereotype.Component;

import cl.tupolera.auth.dto.UsuarioResponse;
import cl.tupolera.auth.model.Usuario;

@Component
public class UsuarioMapper {

    public UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getRun(),
                u.getNombre(),
                u.getApellidos(),
                u.getCorreo(),
                u.getFechaNacimiento(),
                u.getDireccion(),
                u.getRegion().getId(),
                u.getRegion().getNombre(),
                u.getComuna().getId(),
                u.getComuna().getNombre(),
                u.getRol(),
                u.isActivo(),
                u.getFechaCreacion());
    }
}
