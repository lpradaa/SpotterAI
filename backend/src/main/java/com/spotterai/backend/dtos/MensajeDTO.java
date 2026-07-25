package com.spotterai.backend.dtos;

import java.time.LocalDateTime;

public class MensajeDTO {
    
    private Long id;
    private Long emisorId;
    private String emisorNombre;
    private Long receptorId;
    private String contenido;
    private LocalDateTime fechaEnvio;

    // Constructor vacío (Obligatorio para que Spring Boot no se queje)
    public MensajeDTO() {}

    // Constructor completo (El que estamos usando en tu MensajeServiceImpl)
    public MensajeDTO(Long id, Long emisorId, String emisorNombre, Long receptorId, String contenido, LocalDateTime fechaEnvio) {
        this.id = id;
        this.emisorId = emisorId;
        this.emisorNombre = emisorNombre;
        this.receptorId = receptorId;
        this.contenido = contenido;
        this.fechaEnvio = fechaEnvio;
    }

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getEmisorId() { return emisorId; }
    public void setEmisorId(Long emisorId) { this.emisorId = emisorId; }
    
    public String getEmisorNombre() { return emisorNombre; }
    public void setEmisorNombre(String emisorNombre) { this.emisorNombre = emisorNombre; }
    
    public Long getReceptorId() { return receptorId; }
    public void setReceptorId(Long receptorId) { this.receptorId = receptorId; }
    
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }
}