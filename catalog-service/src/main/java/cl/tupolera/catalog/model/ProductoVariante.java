package cl.tupolera.catalog.model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Variante de un producto por talla: tiene su propio stock y sus propias medidas. */
@Entity
@Table(name = "producto_variante")
public class ProductoVariante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, length = 30)
    private String talla;

    @Column(nullable = false)
    private int stock;

    /**
     * Control de concurrencia optimista: si el stock cambia (por ejemplo una venta) mientras
     * se guarda una edición, la edición falla en lugar de pisar el valor.
     */
    @Version
    private Long version;

    /** Medidas de esta talla, por clave (por ejemplo contorno_pecho -> 102). */
    @ElementCollection
    @CollectionTable(name = "variante_medida", joinColumns = @JoinColumn(name = "variante_id"))
    @MapKeyColumn(name = "clave", length = 60)
    @Column(name = "valor", nullable = false, precision = 8, scale = 2)
    private Map<String, BigDecimal> medidas = new LinkedHashMap<>();

    protected ProductoVariante() {
    }

    public ProductoVariante(Producto producto, String talla) {
        this.producto = producto;
        this.talla = talla;
    }

    public Long getId() { return id; }
    public Producto getProducto() { return producto; }
    public String getTalla() { return talla; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public Long getVersion() { return version; }
    public Map<String, BigDecimal> getMedidas() { return medidas; }
}
