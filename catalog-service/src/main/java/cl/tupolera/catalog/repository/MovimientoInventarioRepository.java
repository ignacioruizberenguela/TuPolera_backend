package cl.tupolera.catalog.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.tupolera.catalog.model.MovimientoInventario;
import cl.tupolera.catalog.model.TipoMovimiento;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    Optional<MovimientoInventario> findByTipoAndClave(TipoMovimiento tipo, String clave);

    boolean existsByTipoAndClave(TipoMovimiento tipo, String clave);
}
