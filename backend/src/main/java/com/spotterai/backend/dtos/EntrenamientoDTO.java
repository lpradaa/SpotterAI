package com.spotterai.backend.dtos;

public class EntrenamientoDTO {

    /**
     * Hace falta para poder borrarlo.
     *
     * <p>No salia, asi que la lista del tablero no tenia forma de referirse a
     * uno en concreto: un dedazo en la fecha o un duplicado era permanente. Y
     * esto no es un dato declarado como el nivel, es el unico dato medido del
     * motor —alimenta la constancia, diez puntos con todo el mundo—, o sea que
     * su integridad importa mas que la de los demas.
     */
    private Long id;

    private String fecha;
    private String tipo;
    private Integer duracionMinutos;
    private String lugarONotas;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public String getLugarONotas() { return lugarONotas; }
    public void setLugarONotas(String lugarONotas) { this.lugarONotas = lugarONotas; }
}