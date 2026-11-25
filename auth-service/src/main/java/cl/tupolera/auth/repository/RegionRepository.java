package cl.tupolera.auth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.tupolera.auth.model.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByOrderByIdAsc();
}
