package cl.tupolera.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Línea de un movimiento: qué variante se afectó, cuántas unidades y el stock resultante. */
@Embeddable
public class DetalleMovimiento {

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "variante_id", nullable = false)
    private Long varianteId;

    @Column(nullable = false, length = 30)
    private String talla;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "stock_despues", nullable = false)
    private int stockDespues;

    protected DetalleMovimiento() {
    }

    public DetalleMovimiento(Long productoId, Long varianteId, String talla, int cantidad, int stockDespues) {
        this.productoId = productoId;
        this.varianteId = varianteId;
        this.talla = talla;
        this.cantidad = cantidad;
        this.stockDespues = stockDespues;
    }

    public Long getProductoId() { return productoId; }
    public Long getVarianteId() { return varianteId; }
    public String getTalla() { return talla; }
    public int getCantidad() { return cantidad; }
    public int getStockDespues() { return stockDespues; }
}
