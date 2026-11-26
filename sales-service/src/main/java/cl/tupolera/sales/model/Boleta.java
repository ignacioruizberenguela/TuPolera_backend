package cl.tupolera.sales.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Una compra con su fotografía histórica (comprador, destino, bodega y montos). Los montos son CLP enteros:
 * subtotal = productos con IVA incluido; neto + iva = subtotal; total = subtotal + precioDespacho.
 * El despacho va aparte y no lleva IVA en este cálculo.
 */
@Entity
@Table(name = "boleta")
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "numero_boleta")
    private String numeroBoleta;
    @Column(name = "numero_operacion", nullable = false)
    private String numeroOperacion;
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;
    @Column(name = "comprador_nombre", nullable = false)
    private String compradorNombre;
    @Column(name = "comprador_correo", nullable = false)
    private String compradorCorreo;
    @Column(name = "comprador_run")
    private String compradorRun;
    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoBoleta estado;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_inventario", nullable = false)
    private EstadoInventario estadoInventario;
    @Column(name = "motivo_rechazo")
    private String motivoRechazo;
    @Enumerated(EnumType.STRING)
    @Column(name = "resultado_pago", nullable = false)
    private ResultadoPago resultadoPago;
    @Column(name = "referencia_descuento")
    private String referenciaDescuento;
    @Column(name = "clave_idempotencia", nullable = false)
    private String claveIdempotencia;
    @Column(name = "huella_solicitud", nullable = false)
    private String huellaSolicitud;
    @Column(name = "subtotal", nullable = false)
    private long subtotal;
    @Column(name = "precio_despacho", nullable = false)
    private long precioDespacho;
    @Column(name = "total", nullable = false)
    private long total;
    @Column(name = "neto", nullable = false)
    private long neto;
    @Column(name = "iva", nullable = false)
    private long iva;
    @Column(name = "despacho_direccion", nullable = false)
    private String despachoDireccion;
    @Column(name = "despacho_latitud", nullable = false)
    private double despachoLatitud;
    @Column(name = "despacho_longitud", nullable = false)
    private double despachoLongitud;
    @Column(name = "despacho_distancia_km", nullable = false)
    private BigDecimal despachoDistanciaKm;
    @Column(name = "despacho_tipo_calculo", nullable = false)
    private String despachoTipoCalculo;
    @Column(name = "origen_nombre", nullable = false)
    private String origenNombre;
    @Column(name = "origen_direccion", nullable = false)
    private String origenDireccion;
    @Column(name = "origen_latitud", nullable = false)
    private double origenLatitud;
    @Column(name = "origen_longitud", nullable = false)
    private double origenLongitud;
    @Version
    private Long version;

    @OneToMany(mappedBy = "boleta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<BoletaDetalle> detalles = new ArrayList<>();

    protected Boleta() {
    }

    /** Compra nueva. El número BOL-0001 se asigna después de insertar (ver asignarNumero). */
    public Boleta(Long usuarioId, String numeroOperacion, String claveIdempotencia, String huellaSolicitud,
            ResultadoPago resultadoPago) {
        this.usuarioId = usuarioId;
        this.numeroOperacion = numeroOperacion;
        this.claveIdempotencia = claveIdempotencia;
        this.huellaSolicitud = huellaSolicitud;
        this.resultadoPago = resultadoPago;
        // Estado inicial seguro: por confirmar. Cumple las restricciones del esquema hasta que se decida.
        this.estado = EstadoBoleta.PENDIENTE;
        this.estadoInventario = EstadoInventario.PENDIENTE;
        this.fecha = LocalDateTime.now(ZoneOffset.UTC);
        this.fechaActualizacion = this.fecha;
    }

    public void agregarDetalle(BoletaDetalle detalle) {
        detalle.setBoleta(this);
        detalles.add(detalle);
    }

    /** Aprobada solo con inventario confirmado: el esquema impide otra combinación. */
    public void cambiarEstado(EstadoBoleta nuevoEstado, EstadoInventario nuevoInventario, String motivoRechazo) {
        if (nuevoEstado == EstadoBoleta.APROBADA && nuevoInventario != EstadoInventario.DESCONTADO) {
            throw new IllegalStateException(
                    "Una boleta solo puede aprobarse con el inventario DESCONTADO (recibido: " + nuevoInventario + ")");
        }
        this.estado = nuevoEstado;
        this.estadoInventario = nuevoInventario;
        this.motivoRechazo = motivoRechazo;
        this.fechaActualizacion = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void asignarNumero() {
        if (id != null && numeroBoleta == null) {
            this.numeroBoleta = String.format("BOL-%04d", id);
        }
    }

    public List<BoletaDetalle> getDetalles() { return detalles; }

    public Long getId() { return id; }
    public String getNumeroBoleta() { return numeroBoleta; }
    public void setNumeroBoleta(String numeroBoleta) { this.numeroBoleta = numeroBoleta; }
    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String numeroOperacion) { this.numeroOperacion = numeroOperacion; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getCompradorNombre() { return compradorNombre; }
    public void setCompradorNombre(String compradorNombre) { this.compradorNombre = compradorNombre; }
    public String getCompradorCorreo() { return compradorCorreo; }
    public void setCompradorCorreo(String compradorCorreo) { this.compradorCorreo = compradorCorreo; }
    public String getCompradorRun() { return compradorRun; }
    public void setCompradorRun(String compradorRun) { this.compradorRun = compradorRun; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
    public EstadoBoleta getEstado() { return estado; }
    public void setEstado(EstadoBoleta estado) { this.estado = estado; }
    public EstadoInventario getEstadoInventario() { return estadoInventario; }
    public void setEstadoInventario(EstadoInventario estadoInventario) { this.estadoInventario = estadoInventario; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }
    public ResultadoPago getResultadoPago() { return resultadoPago; }
    public void setResultadoPago(ResultadoPago resultadoPago) { this.resultadoPago = resultadoPago; }
    public String getReferenciaDescuento() { return referenciaDescuento; }
    public void setReferenciaDescuento(String referenciaDescuento) { this.referenciaDescuento = referenciaDescuento; }
    public String getClaveIdempotencia() { return claveIdempotencia; }
    public void setClaveIdempotencia(String claveIdempotencia) { this.claveIdempotencia = claveIdempotencia; }
    public String getHuellaSolicitud() { return huellaSolicitud; }
    public void setHuellaSolicitud(String huellaSolicitud) { this.huellaSolicitud = huellaSolicitud; }
    public long getSubtotal() { return subtotal; }
    public void setSubtotal(long subtotal) { this.subtotal = subtotal; }
    public long getPrecioDespacho() { return precioDespacho; }
    public void setPrecioDespacho(long precioDespacho) { this.precioDespacho = precioDespacho; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getNeto() { return neto; }
    public void setNeto(long neto) { this.neto = neto; }
    public long getIva() { return iva; }
    public void setIva(long iva) { this.iva = iva; }
    public String getDespachoDireccion() { return despachoDireccion; }
    public void setDespachoDireccion(String despachoDireccion) { this.despachoDireccion = despachoDireccion; }
    public double getDespachoLatitud() { return despachoLatitud; }
    public void setDespachoLatitud(double despachoLatitud) { this.despachoLatitud = despachoLatitud; }
    public double getDespachoLongitud() { return despachoLongitud; }
    public void setDespachoLongitud(double despachoLongitud) { this.despachoLongitud = despachoLongitud; }
    public BigDecimal getDespachoDistanciaKm() { return despachoDistanciaKm; }
    public void setDespachoDistanciaKm(BigDecimal despachoDistanciaKm) { this.despachoDistanciaKm = despachoDistanciaKm; }
    public String getDespachoTipoCalculo() { return despachoTipoCalculo; }
    public void setDespachoTipoCalculo(String despachoTipoCalculo) { this.despachoTipoCalculo = despachoTipoCalculo; }
    public String getOrigenNombre() { return origenNombre; }
    public void setOrigenNombre(String origenNombre) { this.origenNombre = origenNombre; }
    public String getOrigenDireccion() { return origenDireccion; }
    public void setOrigenDireccion(String origenDireccion) { this.origenDireccion = origenDireccion; }
    public double getOrigenLatitud() { return origenLatitud; }
    public void setOrigenLatitud(double origenLatitud) { this.origenLatitud = origenLatitud; }
    public double getOrigenLongitud() { return origenLongitud; }
    public void setOrigenLongitud(double origenLongitud) { this.origenLongitud = origenLongitud; }
    public Long getVersion() { return version; }
}
