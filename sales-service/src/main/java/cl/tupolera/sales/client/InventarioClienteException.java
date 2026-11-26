package cl.tupolera.sales.client;

import java.util.List;

import cl.tupolera.sales.client.dto.ErrorItemInventario;

/**
 * Fallo al operar el inventario en catalog-service. El tipo indica lo MÁS IMPORTANTE para sales-service: si el
 * stock pudo haberse modificado o no.
 *
 * - RECHAZADO: catalog respondió 400, 404 o 409. No se aplicó nada. El detalle va en "errores".
 * - REINTENTABLE: no se aplicó nada y se puede volver a intentar (503 "inventario ocupado", o no se logró ni
 *   conectar con catalog-service).
 * - CONFIGURACION: 401; la clave interna falta o es incorrecta. No se aplicó nada, pero reintentar no sirve.
 * - INCIERTO: no se sabe si se aplicó (tiempo de espera, conexión cortada, respuesta ilegible, error 5xx
 *   distinto de 503). NUNCA debe tratarse como "falló": hay que reintentar con la MISMA referencia, que es
 *   idempotente en catalog-service.
 */
public class InventarioClienteException extends RuntimeException {

    public enum Tipo { RECHAZADO, REINTENTABLE, CONFIGURACION, INCIERTO }

    private final Tipo tipo;
    private final int status;
    private final List<ErrorItemInventario> errores;

    public InventarioClienteException(Tipo tipo, int status, String mensaje, List<ErrorItemInventario> errores,
            Throwable causa) {
        super(mensaje, causa);
        this.tipo = tipo;
        this.status = status;
        this.errores = errores == null ? List.of() : List.copyOf(errores);
    }

    public Tipo getTipo() {
        return tipo;
    }

    /** Código HTTP recibido de catalog-service, o 0 si no hubo respuesta. */
    public int getStatus() {
        return status;
    }

    public List<ErrorItemInventario> getErrores() {
        return errores;
    }

    public boolean tieneCodigo(String codigo) {
        return errores.stream().anyMatch(e -> codigo.equals(e.codigo()));
    }
}
