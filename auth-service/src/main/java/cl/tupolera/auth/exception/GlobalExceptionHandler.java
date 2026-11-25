package cl.tupolera.auth.exception;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
        return construir(ex.getStatus(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<ErrorResponse.CampoInvalido> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorResponse.CampoInvalido(e.getField(), e.getDefaultMessage()))
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la solicitud.", request, detalles);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ErrorResponse> handleCuerpoInvalido(Exception ex, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "La solicitud tiene un formato inválido.", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegridad(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        // Respaldo ante duplicados que se escapen de las validaciones previas (carreras).
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return construir(HttpStatus.CONFLICT, "Los datos entran en conflicto con un registro existente.", request,
                null);
    }

    // Spring Security: deben responder 401/403 y no caer en el manejador genérico (500).
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(AccessDeniedException ex,
            HttpServletRequest request) {
        return construir(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción.", request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleNoAutenticado(AuthenticationException ex,
            HttpServletRequest request) {
        return construir(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión con un token válido.", request, null);
    }

    @ExceptionHandler({ NoHandlerFoundException.class, NoResourceFoundException.class })
    public ResponseEntity<ErrorResponse> handleNoEncontrado(Exception ex, HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, "El recurso solicitado no existe.", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenerico(Exception ex, HttpServletRequest request) {
        // El detalle queda solo en el log del servidor; el cliente no recibe trazas.
        log.error("Error interno en {}", request.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno. Inténtalo más tarde.",
                request, null);
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String mensaje,
            HttpServletRequest request, List<ErrorResponse.CampoInvalido> detalles) {
        ErrorResponse cuerpo = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                mensaje, request.getRequestURI(), detalles);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
