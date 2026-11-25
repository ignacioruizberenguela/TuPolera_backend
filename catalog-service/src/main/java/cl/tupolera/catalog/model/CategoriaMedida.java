package cl.tupolera.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Medida que corresponde a una categoría (por ejemplo "Contorno de pecho", en cm). */
@Entity
@Table(name = "categoria_medida")
public class CategoriaMedida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, length = 60)
    private String clave;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String unidad;

    /** Si es verdadero, el cliente ingresa esta medida en el comparador de medidas. */
    @Column(name = "solicitar_cliente", nullable = false)
    private boolean solicitarCliente;

    /** Si es verdadero, debe registrarse para cada talla de cada producto. */
    @Column(name = "registrar_producto", nullable = false)
    private boolean registrarProducto = true;

    @Column(nullable = false)
    private int orden;

    protected CategoriaMedida() {
    }

    public CategoriaMedida(Categoria categoria, String clave) {
        this.categoria = categoria;
        this.clave = clave;
    }

    public Long getId() { return id; }
    public Categoria getCategoria() { return categoria; }
    public String getClave() { return clave; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public boolean isSolicitarCliente() { return solicitarCliente; }
    public void setSolicitarCliente(boolean solicitarCliente) { this.solicitarCliente = solicitarCliente; }
    public boolean isRegistrarProducto() { return registrarProducto; }
    public void setRegistrarProducto(boolean registrarProducto) { this.registrarProducto = registrarProducto; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
