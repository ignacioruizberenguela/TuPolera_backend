package cl.tupolera.auth.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Refresh token almacenado como hash SHA-256: el valor original solo existe
 * en la respuesta enviada al cliente.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    private boolean revocado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    protected RefreshToken() {
    }

    public RefreshToken(Long usuarioId, String tokenHash, LocalDateTime fechaExpiracion) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.fechaExpiracion = fechaExpiracion;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public boolean isRevocado() { return revocado; }
    public void setRevocado(boolean revocado) { this.revocado = revocado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    public boolean estaExpirado() {
        return fechaExpiracion.isBefore(LocalDateTime.now());
    }
}
