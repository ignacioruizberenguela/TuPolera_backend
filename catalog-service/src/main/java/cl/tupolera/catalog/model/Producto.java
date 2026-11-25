package cl.tupolera.catalog.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Producto con campos comunes (nombre, precio, color, descuento...) y una parte dinámica:
 * atributos propios de su categoría y variantes por talla (cada una con su stock y sus medidas).
 */
@Entity
@Table(name = "producto")
public class Producto {

    public static final int STOCK_CRITICO_POR_DEFECTO = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** Precio en pesos chilenos (CLP no usa decimales) con IVA incluido. */
    @Column(nullable = false)
    private int precio;

    /** Porcentaje de descuento (0 a 100). */
    @Column(nullable = false)
    private int descuento;

    @Column(length = 40)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    /** Nombre de una imagen del frontend o una imagen subida (data URL). Columna LONGTEXT. */
    @Column(columnDefinition = "LONGTEXT")
    private String imagen;

    @Column(name = "stock_critico", nullable = false)
    private int stockCritico = STOCK_CRITICO_POR_DEFECTO;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ProductoVariante> variantes = new ArrayList<>();

    /** Valores de los atributos de la categoría, por clave (por ejemplo tipo_manga -> Corta). */
    @ElementCollection
    @CollectionTable(name = "producto_atributo", joinColumns = @JoinColumn(name = "producto_id"))
    @MapKeyColumn(name = "clave", length = 60)
    @Column(name = "valor", nullable = false, length = 255)
    private Map<String, String> atributos = new LinkedHashMap<>();

    @PrePersist
    void antesDeGuardar() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = ahora;
        }
    }

    /** Stock general: suma del stock de todas las variantes (tallas). */
    public int getStockTotal() {
        return variantes.stream().mapToInt(ProductoVariante::getStock).sum();
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public int getPrecio() { return precio; }
    public void setPrecio(int precio) { this.precio = precio; }
    public int getDescuento() { return descuento; }
    public void setDescuento(int descuento) { this.descuento = descuento; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public int getStockCritico() { return stockCritico; }
    public void setStockCritico(int stockCritico) { this.stockCritico = stockCritico; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
    public List<ProductoVariante> getVariantes() { return variantes; }
    public Map<String, String> getAtributos() { return atributos; }
}
