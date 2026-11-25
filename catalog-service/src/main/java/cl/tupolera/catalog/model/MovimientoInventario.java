package cl.tupolera.catalog.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Registro de una operación de inventario aplicada con éxito. La restricción única (tipo, clave) es lo que
 * impide aplicar dos veces el mismo descuento, la misma reposición o la misma reversión, incluso si llegan
 * solicitudes simultáneas. Las operaciones que fallan se revierten y NO dejan registro (se pueden reintentar).
 */
@Entity
@Table(name = "movimiento_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoMovimiento tipo;

    /** Referencia/clave de idempotencia enviada por el servicio llamador (recomendado: UUID). */
    @Column(nullable = false, length = 64)
    private String clave;

    /** SHA-256 de los ítems de la solicitud: detecta si la misma clave se reutiliza con otro contenido. */
    @Column(nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String huella;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @ElementCollection
    @CollectionTable(name = "movimiento_inventario_detalle", joinColumns = @JoinColumn(name = "movimiento_id"))
    @OrderBy("varianteId ASC")
    private List<DetalleMovimiento> detalles = new ArrayList<>();

    protected MovimientoInventario() {
    }

    public MovimientoInventario(TipoMovimiento tipo, String clave, String huella) {
        this.tipo = tipo;
        this.clave = clave;
        this.huella = huella;
    }

    public Long getId() { return id; }
    public TipoMovimiento getTipo() { return tipo; }
    public String getClave() { return clave; }
    public String getHuella() { return huella; }
    public LocalDateTime getFecha() { return fecha; }
    public List<DetalleMovimiento> getDetalles() { return detalles; }
}
