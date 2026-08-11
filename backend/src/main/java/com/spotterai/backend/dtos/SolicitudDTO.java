package com.spotterai.backend.dtos;

public class SolicitudDTO {
    private Long id;
    private Long emisorId;
    private String emisorNombre;
    private Long receptorId;
    private String receptorNombre;
    private String estado; // PENDIENTE, ACEPTADA, RECHAZADA

    /**
     * La compatibilidad congelada al mandarse, para poder enseñarla.
     *
     * <p>La entidad la guarda desde que existe el embudo, pero no salia de la
     * base: la bandeja decia "Quiere conectar contigo para entrenar" y ya. O
     * sea que el sitio donde de verdad se decide —aceptar a alguien o no— era
     * el unico de toda la aplicacion donde el motor no decia nada, justo el
     * paso que /embudo mide para saber si el motor sirve.
     *
     * <p>{@code null} en las anteriores a que se guardara: la pantalla se calla
     * en vez de inventarse un numero.
     */
    private Integer compatibilidad;

    /** El color de identidad de quien la manda, para que su avatar sea el suyo. */
    private String emisorAvatar;

    /** Su foto, si la tiene. Sin esto la bandeja pintaba iniciales a todo el mundo. */
    private String emisorFotoUrl;

    // Constructor vacío
    public SolicitudDTO() {}

    // Constructor completo
    public SolicitudDTO(Long id, Long emisorId, String emisorNombre, Long receptorId, String receptorNombre, String estado) {
        this.id = id;
        this.emisorId = emisorId;
        this.emisorNombre = emisorNombre;
        this.receptorId = receptorId;
        this.receptorNombre = receptorNombre;
        this.estado = estado;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmisorId() { return emisorId; }
    public void setEmisorId(Long emisorId) { this.emisorId = emisorId; }
    public String getEmisorNombre() { return emisorNombre; }
    public void setEmisorNombre(String emisorNombre) { this.emisorNombre = emisorNombre; }
    public Long getReceptorId() { return receptorId; }
    public void setReceptorId(Long receptorId) { this.receptorId = receptorId; }
    public String getReceptorNombre() { return receptorNombre; }
    public void setReceptorNombre(String receptorNombre) { this.receptorNombre = receptorNombre; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getCompatibilidad() { return compatibilidad; }
    public void setCompatibilidad(Integer compatibilidad) { this.compatibilidad = compatibilidad; }
    public String getEmisorAvatar() { return emisorAvatar; }
    public void setEmisorAvatar(String emisorAvatar) { this.emisorAvatar = emisorAvatar; }
    public String getEmisorFotoUrl() { return emisorFotoUrl; }
    public void setEmisorFotoUrl(String emisorFotoUrl) { this.emisorFotoUrl = emisorFotoUrl; }
}
