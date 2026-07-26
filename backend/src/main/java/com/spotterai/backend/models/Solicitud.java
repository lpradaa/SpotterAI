package com.spotterai.backend.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Solicitud de conexion entre dos usuarios.
 *
 * <p>La restriccion unica sobre (emisor, receptor) es la que faltaba: hubo
 * duplicados y se parchearon en Java, con el {@code findFirstBy...} y el
 * {@code HashSet} que en el servicio se llama "la solucion al misterio de los
 * clones". Eso no evitaba el duplicado, solo lo escondia al leerlo.
 *
 * <p>Cubre el mismo par en el mismo sentido. El caso inverso —A pide a B y B
 * pide a A— sigue comprobandose en {@code SolicitudServiceImpl}, porque
 * expresarlo en la base necesita columnas generadas con LEAST/GREATEST y eso
 * pide una migracion de verdad, no {@code ddl-auto=update}.
 */
@Entity
@Table(
        name = "Solicitud",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_solicitud_emisor_receptor",
                columnNames = {"emisor_id", "receptor_id"}))
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String estado; // PENDIENTE, ACEPTADA, RECHAZADA

    private LocalDateTime fechaSolicitud;

    @ManyToOne
    @JoinColumn(name = "emisor_id")
    private Usuario emisor;

    @ManyToOne
    @JoinColumn(name = "receptor_id")
    private Usuario receptor;

    public Solicitud() {
        this.fechaSolicitud = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public Usuario getEmisor() {
        return emisor;
    }

    public void setEmisor(Usuario emisor) {
        this.emisor = emisor;
    }

    public Usuario getReceptor() {
        return receptor;
    }

    public void setReceptor(Usuario receptor) {
        this.receptor = receptor;
    }
}
    

