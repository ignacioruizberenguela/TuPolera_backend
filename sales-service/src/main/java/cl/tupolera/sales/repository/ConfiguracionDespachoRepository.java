package cl.tupolera.sales.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.tupolera.sales.model.ConfiguracionDespacho;

public interface ConfiguracionDespachoRepository extends JpaRepository<ConfiguracionDespacho, Long> {
}
