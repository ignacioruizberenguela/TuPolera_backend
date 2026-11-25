package cl.tupolera.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.tupolera.catalog.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findAllByOrderByIdAsc();

    List<Producto> findByActivoTrueOrderByIdAsc();

    List<Producto> findByCategoriaIdOrderByIdAsc(Long categoriaId);

    List<Producto> findByCategoriaIdAndActivoTrueOrderByIdAsc(Long categoriaId);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    long countByCategoriaId(Long categoriaId);

    long countByCategoriaIdAndActivoTrue(Long categoriaId);
}
