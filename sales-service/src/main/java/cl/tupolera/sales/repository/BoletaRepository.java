package cl.tupolera.sales.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import cl.tupolera.sales.model.Boleta;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {

    /** Búsqueda de idempotencia: la misma clave del mismo usuario es la misma compra. */
    @EntityGraph(attributePaths = "detalles")
    Optional<Boleta> findByUsuarioIdAndClaveIdempotencia(Long usuarioId, String claveIdempotencia);

    @EntityGraph(attributePaths = "detalles")
    Optional<Boleta> findWithDetallesById(Long id);

    Page<Boleta> findByUsuarioId(Long usuarioId, Pageable pageable);
}
