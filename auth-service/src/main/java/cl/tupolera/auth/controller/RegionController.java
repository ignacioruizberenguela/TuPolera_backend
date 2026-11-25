package cl.tupolera.auth.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.tupolera.auth.dto.ComunaResponse;
import cl.tupolera.auth.dto.RegionResponse;
import cl.tupolera.auth.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/regiones")
@Tag(name = "Regiones y comunas", description = "Consulta pública para los formularios de registro")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping
    @Operation(summary = "Listar regiones")
    public List<RegionResponse> regiones() {
        return regionService.listarRegiones();
    }

    @GetMapping("/{id}/comunas")
    @Operation(summary = "Listar las comunas de una región")
    public List<ComunaResponse> comunas(@PathVariable Long id) {
        return regionService.listarComunas(id);
    }
}
