package cl.tupolera.sales.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.tupolera.sales.dto.ConfiguracionDespachoRequest;
import cl.tupolera.sales.dto.ConfiguracionDespachoResponse;
import cl.tupolera.sales.dto.CotizacionResponse;
import cl.tupolera.sales.dto.DestinoRequest;
import cl.tupolera.sales.security.SecurityUtils;
import cl.tupolera.sales.service.DespachoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/despacho")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Despacho", description = "Configuración de la bodega y tarifas, y cotización del despacho")
public class DespachoController {

    private final DespachoService despachoService;

    public DespachoController(DespachoService despachoService) {
        this.despachoService = despachoService;
    }

    @GetMapping("/configuracion")
    @Operation(summary = "Consultar la configuración de despacho",
            description = "Bodega de origen, precio base, costo por km y tarifas mínima y máxima. Requiere estar autenticado.")
    @ApiResponse(responseCode = "401", description = "Sin token válido")
    public ConfiguracionDespachoResponse consultarConfiguracion() {
        return despachoService.consultarConfiguracion();
    }

    @PutMapping("/configuracion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Modificar la configuración de despacho (solo ADMINISTRADOR)")
    @ApiResponse(responseCode = "400", description = "Datos inválidos (por ejemplo bodega fuera de Chile o máxima menor que mínima)")
    @ApiResponse(responseCode = "401", description = "Sin token válido")
    @ApiResponse(responseCode = "403", description = "Solo ADMINISTRADOR")
    public ConfiguracionDespachoResponse actualizarConfiguracion(
            @Valid @RequestBody ConfiguracionDespachoRequest request) {
        return despachoService.actualizarConfiguracion(request, SecurityUtils.idUsuarioActual());
    }

    @PostMapping("/cotizacion")
    @Operation(summary = "Cotizar el despacho de un destino",
            description = "Recibe la dirección y las coordenadas elegidas en el mapa. El backend calcula la distancia "
                    + "en línea recta (Haversine) desde la bodega y el precio; no acepta una distancia enviada por el cliente. "
                    + "Solo se despacha dentro de Chile.")
    @ApiResponse(responseCode = "400", description = "Dirección vacía, coordenadas inválidas o fuera de Chile")
    @ApiResponse(responseCode = "401", description = "Sin token válido")
    public CotizacionResponse cotizar(@Valid @RequestBody DestinoRequest request) {
        return despachoService.cotizar(request);
    }
}
