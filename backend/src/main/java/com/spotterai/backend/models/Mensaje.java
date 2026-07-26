package com.spotterai.backend.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes")
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenido;

    private LocalDateTime fechaEnvio;

    @ManyToOne
    @JoinColumn(name = "emisor_id", nullable = false)
    private Usuario emisor;

    @ManyToOne
    @JoinColumn(name = "receptor_id", nullable = false)
    private Usuario receptor;

    /**
     * Si el receptor ya ha abierto la conversacion despues de recibirlo.
     *
     * Antes el "sin leer" solo existia en memoria del navegador, asi que
     * desaparecia al recargar: entrabas y no habia forma de saber que te habian
     * escrito. Con columna, sobrevive.
     *
     * Al añadir la columna, MySQL rellena las filas existentes con 0, o sea que
     * todo el historial anterior aparecia como sin leer de golpe. Se marco una
     * vez a mano. Con migraciones esto seria parte del cambio en lugar de un
     * paso suelto que hay que acordarse de repetir en cada entorno.
     */
    @Column(nullable = false)
    private boolean leido = false;


    public Mensaje() {
        this.fechaEnvio = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public Usuario getEmisor() { return emisor; }
    public void setEmisor(Usuario emisor) { this.emisor = emisor; }
    public Usuario getReceptor() { return receptor; }
    public void setReceptor(Usuario receptor) { this.receptor = receptor; }
    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }
}