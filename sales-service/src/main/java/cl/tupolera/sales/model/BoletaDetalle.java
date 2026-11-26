package cl.tupolera.sales.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Fotografía de un producto comprado: no cambia aunque el producto se modifique en el catálogo. */
@Entity
@Table(name = "boleta_detalle")
public class BoletaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "producto_id", nullable = false)
    private Long productoId;
    @Column(name = "codigo_producto")
    private String codigoProducto;
    @Column(name = "nombre_producto", nullable = false)
    private String nombreProducto;
    @Column(name = "imagen")
    private String imagen;
    @Column(name = "imagen_disponible", nullable = false)
    private boolean imagenDisponible;
    @Column(name = "talla", nullable = false)
    private String talla;
    @Column(name = "precio_original", nullable = false)
    private long precioOriginal;
    @Column(name = "descuento", nullable = false)
    private int descuento;
    @Column(name = "precio_final", nullable = false)
    private long precioFinal;
    @Column(name = "cantidad", nullable = false)
    private int cantidad;
    @Column(name = "subtotal", nullable = false)
    private long subtotal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boleta_id", nullable = false)
    private Boleta boleta;

    protected BoletaDetalle() {
    }

    /** precioFinal viene del catálogo (ya con descuento); el subtotal de la línea se calcula aquí. */
    public BoletaDetalle(Long productoId, String codigoProducto, String nombreProducto, String talla,
            long precioOriginal, int descuento, long precioFinal, int cantidad) {
        this.productoId = productoId;
        this.codigoProducto = codigoProducto;
        this.nombreProducto = nombreProducto;
        this.talla = talla;
        this.precioOriginal = precioOriginal;
        this.descuento = descuento;
        this.precioFinal = precioFinal;
        this.cantidad = cantidad;
        this.subtotal = Math.multiplyExact(precioFinal, (long) cantidad);
    }

    /** Guarda la imagen solo si hay una (el límite de 1 MB lo aplica PoliticaImagen antes de llamar aquí). */
    public void asignarImagen(String imagen) {
        boolean hay = imagen != null && !imagen.isBlank();
        this.imagen = hay ? imagen : null;
        this.imagenDisponible = hay;
    }

    public Boleta getBoleta() { return boleta; }
    void setBoleta(Boleta boleta) { this.boleta = boleta; }

    public Long getId() { return id; }
    public Long getProductoId() { return productoId; }
    public String getCodigoProducto() { return codigoProducto; }
    public String getNombreProducto() { return nombreProducto; }
    public String getTalla() { return talla; }
    public long getPrecioOriginal() { return precioOriginal; }
    public int getDescuento() { return descuento; }
    public long getPrecioFinal() { return precioFinal; }
    public int getCantidad() { return cantidad; }
    public String getImagen() { return imagen; }
    public boolean isImagenDisponible() { return imagenDisponible; }
    public long getSubtotal() { return subtotal; }
}
