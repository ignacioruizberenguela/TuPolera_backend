package cl.tupolera.auth.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.tupolera.auth.dto.ComunaResponse;
import cl.tupolera.auth.dto.RegionResponse;
import cl.tupolera.auth.exception.ApiException;
import cl.tupolera.auth.repository.ComunaRepository;
import cl.tupolera.auth.repository.RegionRepository;

@Service
public class RegionService {

    private final RegionRepository regionRepository;
    private final ComunaRepository comunaRepository;

    public RegionService(RegionRepository regionRepository, ComunaRepository comunaRepository) {
        this.regionRepository = regionRepository;
        this.comunaRepository = comunaRepository;
    }

    @Transactional(readOnly = true)
    public List<RegionResponse> listarRegiones() {
        return regionRepository.findAllByOrderByIdAsc().stream()
                .map(region -> new RegionResponse(region.getId(), region.getNombre()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComunaResponse> listarComunas(Long regionId) {
        if (!regionRepository.existsById(regionId)) {
            throw ApiException.notFound("La región indicada no existe.");
        }

        return comunaRepository.findByRegionIdOrderByNombreAsc(regionId).stream()
                .map(comuna -> new ComunaResponse(comuna.getId(), comuna.getNombre(), regionId))
                .toList();
    }
}
