package cl.tupolera.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import cl.tupolera.auth.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = {"region", "comuna"})
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    @EntityGraph(attributePaths = {"region", "comuna"})
    Optional<Usuario> findById(Long id);

    @EntityGraph(attributePaths = {"region", "comuna"})
    List<Usuario> findAllByOrderByIdAsc();

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    boolean existsByRun(String run);

    boolean existsByRunAndIdNot(String run, Long id);
}
