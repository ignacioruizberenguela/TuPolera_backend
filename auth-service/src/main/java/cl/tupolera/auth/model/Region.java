package cl.tupolera.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "region")
public class Region {

    @Id
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    protected Region() {
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
