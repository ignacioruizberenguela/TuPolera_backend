package cl.tupolera.catalog.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;

/**
 * Categoría configurable: define las tallas, los atributos y las medidas que usarán
 * sus productos, de modo que agregar una categoría nueva no requiere cambiar el código.
 */
@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    /** Nombre de una imagen del frontend o una imagen subida (data URL). Columna LONGTEXT. */
    @Column(columnDefinition = "LONGTEXT")
    private String imagen;

    @Column(nullable = false)
    private boolean activa = true;

    @ElementCollection
    @CollectionTable(name = "categoria_talla", joinColumns = @JoinColumn(name = "categoria_id"))
    @OrderColumn(name = "orden")
    @Column(name = "talla", nullable = false, length = 30)
    private List<String> tallas = new ArrayList<>();

    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<CategoriaAtributo> atributos = new ArrayList<>();

    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<CategoriaMedida> medidas = new ArrayList<>();

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public List<String> getTallas() { return tallas; }
    public List<CategoriaAtributo> getAtributos() { return atributos; }
    public List<CategoriaMedida> getMedidas() { return medidas; }
}
