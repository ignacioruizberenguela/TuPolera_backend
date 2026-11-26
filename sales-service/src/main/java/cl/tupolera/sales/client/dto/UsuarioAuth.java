package cl.tupolera.sales.client.dto;

/** Campos de GET /api/v1/auth/me que usa sales-service (el resto de la respuesta se ignora). */
public record UsuarioAuth(Long id, String run, String nombre, String apellidos, String correo, boolean activo) {

    /** Nombre completo para el snapshot del comprador en la boleta. */
    public String nombreCompleto() {
        String n = nombre == null ? "" : nombre.trim();
        String a = apellidos == null ? "" : apellidos.trim();
        return (n + " " + a).trim();
    }
}
