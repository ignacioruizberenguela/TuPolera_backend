package cl.tupolera.auth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.tupolera.auth.dto.UsuarioActualizarRequest;
import cl.tupolera.auth.dto.UsuarioCrearRequest;
import cl.tupolera.auth.dto.UsuarioResponse;
import cl.tupolera.auth.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Administración de usuarios: exclusivo del rol ADMINISTRADOR. */
@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuarios", description = "Administración de usuarios (solo ADMINISTRADOR)")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios")
    @ApiResponse(responseCode = "403", description = "Solo ADMINISTRADOR")
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un usuario por ID")
    @ApiResponse(responseCode = "404", description = "El usuario no existe")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear un usuario con cualquier rol")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "409", description = "RUN o correo ya registrado")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioCrearRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un usuario",
            description = "Si no se envía contraseña se conserva la actual. Un administrador no puede cambiar su propio rol ni desactivarse.")
    public UsuarioResponse actualizar(@PathVariable Long id,
            @Valid @RequestBody UsuarioActualizarRequest request, @AuthenticationPrincipal Jwt jwt) {
        return usuarioService.actualizar(id, request, Long.valueOf(jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar un usuario",
            description = "Desactivación lógica: el usuario no podrá iniciar sesión pero se conserva su historial.")
    @ApiResponse(responseCode = "204", description = "Usuario desactivado")
    public ResponseEntity<Void> desactivar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        usuarioService.desactivar(id, Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }
}
