package cl.tupolera.catalog.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.tupolera.catalog.model.ProductoVariante;

/**
 * Operaciones de stock atómicas. Cada método es un único UPDATE condicional: MySQL bloquea la fila,
 * evalúa la condición con el valor ya confirmado y devuelve cuántas filas cambió (0 = no se cumplió).
 * Así dos compras simultáneas nunca pueden vender las mismas unidades.
 */
public interface ProductoVarianteRepository extends JpaRepository<ProductoVariante, Long> {

    /** Descuenta solo si hay stock suficiente. Devuelve 1 si se descontó y 0 si no alcanzaba. */
    @Modifying(flushAutomatically = true)
    @Query("update ProductoVariante v set v.stock = v.stock - :cantidad, v.version = v.version + 1 "
            + "where v.id = :id and v.stock >= :cantidad")
    int descontar(@Param("id") Long id, @Param("cantidad") int cantidad);

    /**
     * Repone unidades solo si el resultado no supera el tope (evita desbordar el entero).
     * "tope" es el stock máximo permitido ANTES de sumar: stockMaximo - cantidad.
     */
    @Modifying(flushAutomatically = true)
    @Query("update ProductoVariante v set v.stock = v.stock + :cantidad, v.version = v.version + 1 "
            + "where v.id = :id and v.stock <= :tope")
    int reponer(@Param("id") Long id, @Param("cantidad") int cantidad, @Param("tope") int tope);

    /** Lee el stock actual directamente de la base (fila [id, stock]). */
    @Query("select v.id, v.stock from ProductoVariante v where v.id in :ids")
    List<Object[]> stocksPorIds(@Param("ids") Collection<Long> ids);
}
