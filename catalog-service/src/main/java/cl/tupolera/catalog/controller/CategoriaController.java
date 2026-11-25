package cl.tupolera.catalog.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.tupolera.catalog.dto.CategoriaRequest;
import cl.tupolera.catalog.dto.CategoriaResponse;
import cl.tupolera.catalog.security.SecurityUtils;
import cl.tupolera.catalog.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorías", description = "Categorías configurables: tallas, atributos y medidas")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @Operation(summary = "Listar categorías",
            description = "Público: solo categorías activas. ADMINISTRADOR y VENDEDOR pueden pedir también las inactivas.")
    public List<CategoriaResponse> listar(
            @Parameter(description = "Solo tiene efecto para ADMINISTRADOR y VENDEDOR")
            @RequestParam(defaultValue = "false") boolean incluirInactivas) {
        return categoriaService.listar(incluirInactivas && SecurityUtils.puedeVerInactivos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una categoría por ID")
    @ApiResponse(responseCode = "404", description = "La categoría no existe (o está inactiva para el solicitante)")
    public CategoriaResponse obtener(@PathVariable Long id) {
        return categoriaService.obtener(id, SecurityUtils.puedeVerInactivos());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crear una categoría (solo ADMINISTRADOR)")
    @ApiResponse(responseCode = "201", description = "Categoría creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "401", description = "Sin token válido")
    @ApiResponse(responseCode = "403", description = "Solo ADMINISTRADOR")
    @ApiResponse(responseCode = "409", description = "Ya existe una categoría con ese nombre")
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Actualizar una categoría (solo ADMINISTRADOR)",
            description = "Los productos existentes conservan sus datos aunque se quiten tallas, atributos o medidas.")
    @ApiResponse(responseCode = "404", description = "La categoría no existe")
    @ApiResponse(responseCode = "409", description = "Nombre repetido o categoría con productos activos que se quiere desactivar")
    public CategoriaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return categoriaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Eliminar o desactivar una categoría (solo ADMINISTRADOR)",
            description = "Con productos activos: 409. Solo con productos inactivos: se desactiva. Sin productos: se elimina.")
    @ApiResponse(responseCode = "204", description = "Categoría eliminada o desactivada")
    @ApiResponse(responseCode = "409", description = "La categoría tiene productos activos")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
