package cl.tupolera.catalog.model;

import java.util.ArrayList;
import java.util.List;

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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/** Característica propia de una categoría (por ejemplo "Tipo de manga": Corta / Larga). */
@Entity
@Table(name = "categoria_atributo")
public class CategoriaAtributo {

    public static final String TEXTO = "texto";
    public static final String NUMERO = "numero";
    public static final String SELECCION = "seleccion";
    public static final String BOOLEANO = "booleano";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    /** Identificador estable (por ejemplo "tipo_manga"); es la clave usada en los productos. */
    @Column(nullable = false, length = 60)
    private String clave;

    @Column(nullable = false, length = 80)
    private String nombre;

    /** texto, numero, seleccion o booleano (mismos valores que usa el frontend). */
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private boolean obligatorio;

    @Column(nullable = false)
    private int orden;

    @ElementCollection
    @CollectionTable(name = "categoria_atributo_opcion", joinColumns = @JoinColumn(name = "atributo_id"))
    @OrderColumn(name = "orden")
    @Column(name = "opcion", nullable = false, length = 80)
    private List<String> opciones = new ArrayList<>();

    protected CategoriaAtributo() {
    }

    public CategoriaAtributo(Categoria categoria, String clave) {
        this.categoria = categoria;
        this.clave = clave;
    }

    public Long getId() { return id; }
    public Categoria getCategoria() { return categoria; }
    public String getClave() { return clave; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public boolean isObligatorio() { return obligatorio; }
    public void setObligatorio(boolean obligatorio) { this.obligatorio = obligatorio; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
    public List<String> getOpciones() { return opciones; }
}
