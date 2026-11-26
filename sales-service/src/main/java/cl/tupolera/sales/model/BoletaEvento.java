package cl.tupolera.sales.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Bitácora de una boleta. También sirve para contar los intentos de descuento (tipo DESCUENTO_INTENTO), sin
 * agregar columnas. Nunca debe guardar JWT, claves internas ni datos sensibles en "detalle".
 */
@Entity
@Table(name = "boleta_evento")
public class BoletaEvento {

    public static final String COMPRA_RECIBIDA = "COMPRA_RECIBIDA";
    public static final String DESCUENTO_INTENTO = "DESCUENTO_INTENTO";
    public static final String DESCUENTO_CONFIRMADO = "DESCUENTO_CONFIRMADO";
    public static final String DESCUENTO_RECHAZADO = "DESCUENTO_RECHAZADO";
    public static final String DESCUENTO_INCIERTO = "DESCUENTO_INCIERTO";
    public static final String REVERSION_SOLICITADA = "REVERSION_SOLICITADA";
    public static final String REVERSION_CONFIRMADA = "REVERSION_CONFIRMADA";
    public static final String COMPRA_RECHAZADA = "COMPRA_RECHAZADA";
    public static final String COMPRA_APROBADA = "COMPRA_APROBADA";

    private static final int MAX_DETALLE = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "boleta_id", nullable = false)
    private Long boletaId;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    @Column(name = "detalle", length = MAX_DETALLE)
    private String detalle;

    protected BoletaEvento() {
    }

    public BoletaEvento(Long boletaId, String tipo, String detalle) {
        this.boletaId = boletaId;
        this.tipo = tipo;
        this.detalle = detalle == null || detalle.length() <= MAX_DETALLE ? detalle : detalle.substring(0, MAX_DETALLE);
        this.fecha = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public Long getBoletaId() { return boletaId; }
    public LocalDateTime getFecha() { return fecha; }
    public String getTipo() { return tipo; }
    public String getDetalle() { return detalle; }
}
