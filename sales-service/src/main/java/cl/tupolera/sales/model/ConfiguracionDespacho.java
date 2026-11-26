package cl.tupolera.sales.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Configuración oficial del despacho (una sola fila, id = 1). Reemplaza a la configuración que antes
 * vivía en localStorage del navegador.
 */
@Entity
@Table(name = "configuracion_despacho")
public class ConfiguracionDespacho {

    public static final long ID_UNICO = 1L;

    @Id
    private Long id = ID_UNICO;

    @Column(name = "nombre_bodega", nullable = false, length = 120)
    private String nombreBodega;

    @Column(name = "direccion_bodega", nullable = false, length = 255)
    private String direccionBodega;

    @Column(name = "latitud_bodega", nullable = false)
    private double latitudBodega;

    @Column(name = "longitud_bodega", nullable = false)
    private double longitudBodega;

    /** En pesos chilenos. */
    @Column(name = "precio_base", nullable = false)
    private long precioBase;

    /** Pesos por cada kilómetro de distancia. */
    @Column(name = "costo_por_km", nullable = false)
    private long costoPorKm;

    @Column(name = "tarifa_minima")
    private Long tarifaMinima;

    @Column(name = "tarifa_maxima")
    private Long tarifaMaxima;

    /** Verdadero mientras la dirección de la bodega sea ficticia (de prueba). */
    @Column(name = "es_ubicacion_prueba", nullable = false)
    private boolean esUbicacionPrueba;

    /** Fecha y hora en UTC. */
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now(ZoneOffset.UTC);

    /** Id del administrador que hizo el último cambio (viene del JWT). */
    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    public Long getId() { return id; }
    public String getNombreBodega() { return nombreBodega; }
    public void setNombreBodega(String nombreBodega) { this.nombreBodega = nombreBodega; }
    public String getDireccionBodega() { return direccionBodega; }
    public void setDireccionBodega(String direccionBodega) { this.direccionBodega = direccionBodega; }
    public double getLatitudBodega() { return latitudBodega; }
    public void setLatitudBodega(double latitudBodega) { this.latitudBodega = latitudBodega; }
    public double getLongitudBodega() { return longitudBodega; }
    public void setLongitudBodega(double longitudBodega) { this.longitudBodega = longitudBodega; }
    public long getPrecioBase() { return precioBase; }
    public void setPrecioBase(long precioBase) { this.precioBase = precioBase; }
    public long getCostoPorKm() { return costoPorKm; }
    public void setCostoPorKm(long costoPorKm) { this.costoPorKm = costoPorKm; }
    public Long getTarifaMinima() { return tarifaMinima; }
    public void setTarifaMinima(Long tarifaMinima) { this.tarifaMinima = tarifaMinima; }
    public Long getTarifaMaxima() { return tarifaMaxima; }
    public void setTarifaMaxima(Long tarifaMaxima) { this.tarifaMaxima = tarifaMaxima; }
    public boolean isEsUbicacionPrueba() { return esUbicacionPrueba; }
    public void setEsUbicacionPrueba(boolean esUbicacionPrueba) { this.esUbicacionPrueba = esUbicacionPrueba; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
    public Long getActualizadoPor() { return actualizadoPor; }
    public void setActualizadoPor(Long actualizadoPor) { this.actualizadoPor = actualizadoPor; }

    /** Configuración inicial: la bodega de prueba de Tu Polera con las tarifas por defecto. */
    public static ConfiguracionDespacho porDefecto() {
        ConfiguracionDespacho c = new ConfiguracionDespacho();
        c.nombreBodega = "Bodega de prueba Tu Polera";
        c.direccionBodega = "Calle Ficticia 1234, Santiago, Región Metropolitana";
        c.latitudBodega = -33.4372;
        c.longitudBodega = -70.6506;
        c.precioBase = 2990;
        c.costoPorKm = 150;
        c.tarifaMinima = 2990L;
        c.tarifaMaxima = 15000L;
        c.esUbicacionPrueba = true;
        return c;
    }
}
