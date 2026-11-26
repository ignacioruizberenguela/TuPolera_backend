package cl.tupolera.sales.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.tupolera.sales.model.BoletaEvento;

public interface BoletaEventoRepository extends JpaRepository<BoletaEvento, Long> {

    /** Cuenta, por ejemplo, los intentos de descuento ya realizados para una boleta. */
    long countByBoletaIdAndTipo(Long boletaId, String tipo);

    List<BoletaEvento> findByBoletaIdOrderByFechaAscIdAsc(Long boletaId);
}
