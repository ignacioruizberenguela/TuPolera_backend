package cl.tupolera.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.tupolera.auth.dto.AuthResponse;
import cl.tupolera.auth.dto.LoginRequest;
import cl.tupolera.auth.dto.RefreshRequest;
import cl.tupolera.auth.dto.RegistroRequest;
import cl.tupolera.auth.dto.UsuarioResponse;
import cl.tupolera.auth.exception.ApiException;
import cl.tupolera.auth.model.Usuario;
import cl.tupolera.auth.repository.UsuarioRepository;

/** Casos de uso de autenticación: registro, login, renovación, cierre de sesión y perfil. */
@Service
public class AuthService {

    private static final String CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos.";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UsuarioMapper mapper;

    public AuthService(UsuarioRepository usuarioRepository, UsuarioService usuarioService,
            PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenService refreshTokenService,
            UsuarioMapper mapper) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.mapper = mapper;
    }

    public UsuarioResponse registrar(RegistroRequest request) {
        return usuarioService.registrarCliente(request);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(request.correo().trim())
                .orElseThrow(() -> ApiException.unauthorized(CREDENCIALES_INVALIDAS));

        // Mismo mensaje para usuario inexistente, inactivo o contraseña errónea (no revela cuál falló).
        if (!usuario.isActivo() || !passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw ApiException.unauthorized(CREDENCIALES_INVALIDAS);
        }

        return construirRespuesta(usuario, refreshTokenService.crear(usuario.getId()));
    }

    /** Renueva el par de tokens. El refresh token usado queda revocado (rotación). */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(RefreshRequest request) {
        RefreshTokenService.Rotacion rotacion = refreshTokenService.rotar(request.refreshToken());

        Usuario usuario = usuarioRepository.findById(rotacion.usuarioId())
                .orElseThrow(() -> ApiException.unauthorized("El refresh token no es válido."));

        if (!usuario.isActivo()) {
            refreshTokenService.revocarTodos(usuario.getId());
            throw ApiException.unauthorized("El usuario está inactivo.");
        }

        return construirRespuesta(usuario, rotacion.nuevoRefreshToken());
    }

    public void logout(RefreshRequest request) {
        refreshTokenService.revocar(request.refreshToken());
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPerfil(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.unauthorized("El usuario del token ya no existe."));

        if (!usuario.isActivo()) {
            throw ApiException.unauthorized("El usuario está inactivo.");
        }

        return mapper.toResponse(usuario);
    }

    private AuthResponse construirRespuesta(Usuario usuario, String refreshToken) {
        return new AuthResponse(
                jwtService.generarAccessToken(usuario),
                refreshToken,
                "Bearer",
                jwtService.segundosDeVidaAccessToken(),
                mapper.toResponse(usuario));
    }
}
