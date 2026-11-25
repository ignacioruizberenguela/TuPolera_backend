package cl.tupolera.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.tupolera.auth.model.Comuna;

public interface ComunaRepository extends JpaRepository<Comuna, Long> {

    List<Comuna> findByRegionIdOrderByNombreAsc(Long regionId);

    Optional<Comuna> findByIdAndRegionId(Long id, Long regionId);
}
