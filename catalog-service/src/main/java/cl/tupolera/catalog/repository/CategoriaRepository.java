package cl.tupolera.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.tupolera.catalog.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByOrderByIdAsc();

    List<Categoria> findByActivaTrueOrderByIdAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
