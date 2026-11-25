package cl.tupolera.catalog.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.tupolera.catalog.dto.ConsultaInventarioRequest;
import cl.tupolera.catalog.dto.ConsultaInventarioResponse;
import cl.tupolera.catalog.dto.DescuentoRequest;
import cl.tupolera.catalog.dto.ReposicionRequest;
import cl.tupolera.catalog.dto.ResultadoInventarioResponse;
import cl.tupolera.catalog.service.InventarioService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;

/**
 * Operaciones internas entre servicios (las usa sales-service). Se protegen con la cabecera X-Internal-Key
 * (ver InternalSecurityConfig), el Gateway no las enruta y no aparecen en Swagger: se documentan en el README
 * y en la colección Postman. Nunca deben exponerse públicamente.
 */
@Hidden
@RestController
@RequestMapping("/internal/inventario")
public class InventarioInternoController {

    private final InventarioService inventarioService;

    public InventarioInternoController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    /** Consulta varios productos con sus variantes, precio vigente y stock. */
    @PostMapping("/consulta")
    public ConsultaInventarioResponse consultar(@Valid @RequestBody ConsultaInventarioRequest request) {
        return inventarioService.consultar(request);
    }

    /** Descuenta varias variantes en una operación atómica e idempotente (por "referencia"). */
    @PostMapping("/descuento")
    public ResultadoInventarioResponse descontar(@Valid @RequestBody DescuentoRequest request) {
        return inventarioService.descontar(request);
    }

    /** Repone unidades de una o más variantes (idempotente por "claveIdempotencia"). */
    @PostMapping("/reposicion")
    public ResultadoInventarioResponse reponer(@Valid @RequestBody ReposicionRequest request) {
        return inventarioService.reponer(request);
    }

    /** Compensación: devuelve exactamente lo descontado por esa referencia, una sola vez. */
    @PostMapping("/descuento/{referencia}/reversion")
    public ResultadoInventarioResponse revertir(@PathVariable String referencia) {
        return inventarioService.revertirDescuento(referencia);
    }
}
