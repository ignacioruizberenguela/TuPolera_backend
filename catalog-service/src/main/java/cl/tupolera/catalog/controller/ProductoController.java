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

import cl.tupolera.catalog.dto.ProductoRequest;
import cl.tupolera.catalog.dto.ProductoResponse;
import cl.tupolera.catalog.security.SecurityUtils;
import cl.tupolera.catalog.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Productos con variantes por talla, atributos dinámicos y stock")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @Operation(summary = "Listar productos",
            description = "Público: solo productos activos. ADMINISTRADOR y VENDEDOR pueden pedir también los inactivos.")
    public List<ProductoResponse> listar(
            @Parameter(description = "Solo tiene efecto para ADMINISTRADOR y VENDEDOR")
            @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return productoService.listar(incluirInactivos && SecurityUtils.puedeVerInactivos());
    }

    @GetMapping("/criticos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Productos con stock crítico (solo ADMINISTRADOR)",
            description = "Productos activos con al menos una talla cuyo stock es menor o igual a su stock crítico. "
                    + "Cada producto indica sus tallasCriticas.")
    @ApiResponse(responseCode = "401", description = "Sin token válido")
    @ApiResponse(responseCode = "403", description = "Solo ADMINISTRADOR")
    public List<ProductoResponse> criticos() {
        return productoService.criticos();
    }

    @GetMapping("/categoria/{categoriaId}")
    @Operation(summary = "Filtrar productos por categoría")
    @ApiResponse(responseCode = "404", description = "La categoría no existe")
    public List<ProductoResponse> porCategoria(@PathVariable Long categoriaId,
            @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return productoService.listarPorCategoria(categoriaId, incluirInactivos && SecurityUtils.puedeVerInactivos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un producto por ID")
    @ApiResponse(responseCode = "404", description = "El producto no existe (o está inactivo para el solicitante)")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id, SecurityUtils.puedeVerInactivos());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crear un producto (solo ADMINISTRADOR)",
            description = "Los atributos, tallas y medidas aceptados dependen de la categoría elegida.")
    @ApiResponse(responseCode = "201", description = "Producto creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o que no cumplen la configuración de la categoría")
    @ApiResponse(responseCode = "401", description = "Sin token válido")
    @ApiResponse(responseCode = "403", description = "Solo ADMINISTRADOR")
    @ApiResponse(responseCode = "409", description = "Ya existe un producto con ese código")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Actualizar un producto (solo ADMINISTRADOR)",
            description = "Reemplaza los datos del producto, incluyendo stock y medidas por talla. "
                    + "Con activo = true se reactiva un producto desactivado.")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    @ApiResponse(responseCode = "409", description = "Código repetido")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Desactivar un producto (solo ADMINISTRADOR)",
            description = "Desactivación lógica: el producto puede tener boletas históricas, por lo que no se borra.")
    @ApiResponse(responseCode = "204", description = "Producto desactivado")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        productoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
