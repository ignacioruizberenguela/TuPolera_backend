package cl.tupolera.catalog.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

import cl.tupolera.catalog.dto.ErrorItemInventario;

/** Error de una operación de inventario, con el detalle de cada ítem que falló. */
public class InventarioException extends RuntimeException {

    private final HttpStatus status;
    private final List<ErrorItemInventario> errores;

    public InventarioException(HttpStatus status, String message, List<ErrorItemInventario> errores) {
        super(message);
        this.status = status;
        this.errores = errores;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<ErrorItemInventario> getErrores() {
        return errores;
    }
}
