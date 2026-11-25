package cl.tupolera.auth.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.tupolera.auth.dto.RegistroRequest;
import cl.tupolera.auth.dto.UsuarioActualizarRequest;
import cl.tupolera.auth.dto.UsuarioCrearRequest;
import cl.tupolera.auth.dto.UsuarioResponse;
import cl.tupolera.auth.exception.ApiException;
import cl.tupolera.auth.model.Comuna;
import cl.tupolera.auth.model.Region;
import cl.tupolera.auth.model.Rol;
import cl.tupolera.auth.model.Usuario;
import cl.tupolera.auth.repository.ComunaRepository;
import cl.tupolera.auth.repository.RegionRepository;
import cl.tupolera.auth.repository.UsuarioRepository;
import cl.tupolera.auth.util.RunUtil;

/** Reglas de negocio de usuarios: registro, administración y validaciones de unicidad. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RegionRepository regionRepository;
    private final ComunaRepository comunaRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UsuarioMapper mapper;

    public UsuarioService(UsuarioRepository usuarioRepository, RegionRepository regionRepository,
            ComunaRepository comunaRepository, PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService, UsuarioMapper mapper) {
        this.usuarioRepository = usuarioRepository;
        this.regionRepository = regionRepository;
        this.comunaRepository = comunaRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.mapper = mapper;
    }

    /** Registro público: el rol es siempre CLIENTE, sin importar lo que envíe el cliente. */
    @Transactional
    public UsuarioResponse registrarCliente(RegistroRequest req) {
        return crearUsuario(req.run(), req.nombre(), req.apellidos(), req.correo(), req.password(),
                req.fechaNacimiento(), req.direccion(), req.idRegion(), req.idComuna(), Rol.CLIENTE, true);
    }

    @Transactional
    public UsuarioResponse crear(UsuarioCrearRequest req) {
        return crearUsuario(req.run(), req.nombre(), req.apellidos(), req.correo(), req.password(),
                req.fechaNacimiento(), req.direccion(), req.idRegion(), req.idComuna(), req.rol(),
                req.activo() == null || req.activo());
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAllByOrderByIdAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return mapper.toResponse(buscar(id));
    }

    /**
     * @param idSolicitante id del administrador autenticado (tomado del JWT). Un administrador no
     *                      puede cambiar su propio rol ni desactivar su propia cuenta.
     */
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioActualizarRequest req, Long idSolicitante) {
        Usuario usuario = buscar(id);

        String run = RunUtil.normalizar(req.run());
        String correo = normalizarCorreo(req.correo());

        if (usuarioRepository.existsByRunAndIdNot(run, id)) {
            throw ApiException.conflict("Ya existe un usuario con ese RUN.");
        }
        if (usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(correo, id)) {
            throw ApiException.conflict("Ya existe un usuario con ese correo.");
        }

        boolean cambiaRol = usuario.getRol() != req.rol();
        boolean desactiva = usuario.isActivo() && !req.activo();

        if (id.equals(idSolicitante) && (cambiaRol || !req.activo())) {
            throw ApiException.conflict("No puedes cambiar tu propio rol ni desactivar tu propia cuenta.");
        }

        Region region = buscarRegion(req.idRegion());
        Comuna comuna = buscarComuna(req.idComuna(), region.getId());

        usuario.setRun(run);
        usuario.setNombre(req.nombre().trim());
        usuario.setApellidos(limpiar(req.apellidos()));
        usuario.setCorreo(correo);
        usuario.setFechaNacimiento(req.fechaNacimiento());
        usuario.setDireccion(limpiar(req.direccion()));
        usuario.setRegion(region);
        usuario.setComuna(comuna);
        usuario.setRol(req.rol());
        usuario.setActivo(req.activo());

        boolean cambiaPassword = req.password() != null && !req.password().isBlank();
        if (cambiaPassword) {
            usuario.setPassword(passwordEncoder.encode(req.password()));
        }

        usuarioRepository.save(usuario);

        // Los cambios sensibles cierran las sesiones abiertas (el refresh token deja de servir).
        if (cambiaPassword || cambiaRol || desactiva) {
            refreshTokenService.revocarTodos(usuario.getId());
        }

        return mapper.toResponse(usuario);
    }

    /** DELETE es una desactivación lógica: el usuario puede tener boletas históricas asociadas. */
    @Transactional
    public void desactivar(Long id, Long idSolicitante) {
        Usuario usuario = buscar(id);

        if (id.equals(idSolicitante)) {
            throw ApiException.conflict("No puedes desactivar tu propia cuenta.");
        }

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        refreshTokenService.revocarTodos(id);
    }

    private UsuarioResponse crearUsuario(String run, String nombre, String apellidos, String correo,
            String password, LocalDate fechaNacimiento, String direccion, Long idRegion, Long idComuna,
            Rol rol, boolean activo) {

        String runNormalizado = RunUtil.normalizar(run);
        String correoNormalizado = normalizarCorreo(correo);

        if (usuarioRepository.existsByRun(runNormalizado)) {
            throw ApiException.conflict("Ya existe un usuario con ese RUN.");
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(correoNormalizado)) {
            throw ApiException.conflict("Ya existe un usuario con ese correo.");
        }

        Region region = buscarRegion(idRegion);
        Comuna comuna = buscarComuna(idComuna, region.getId());

        Usuario usuario = new Usuario();
        usuario.setRun(runNormalizado);
        usuario.setNombre(nombre.trim());
        usuario.setApellidos(limpiar(apellidos));
        usuario.setCorreo(correoNormalizado);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setFechaNacimiento(fechaNacimiento);
        usuario.setDireccion(limpiar(direccion));
        usuario.setRegion(region);
        usuario.setComuna(comuna);
        usuario.setRol(rol);
        usuario.setActivo(activo);

        return mapper.toResponse(usuarioRepository.save(usuario));
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("El usuario no existe."));
    }

    private Region buscarRegion(Long idRegion) {
        return regionRepository.findById(idRegion)
                .orElseThrow(() -> ApiException.badRequest("La región indicada no existe."));
    }

    private Comuna buscarComuna(Long idComuna, Long idRegion) {
        return comunaRepository.findByIdAndRegionId(idComuna, idRegion)
                .orElseThrow(() -> ApiException.badRequest("La comuna no existe o no pertenece a la región indicada."));
    }

    private static String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase();
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
