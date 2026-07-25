package com.spotterai.backend.dtos;

public class EntrenamientoDTO {
    private String fecha;
    private String tipo;
    private Integer duracionMinutos;
    private String lugarONotas;

    // Getters y Setters
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public String getLugarONotas() { return lugarONotas; }
    public void setLugarONotas(String lugarONotas) { this.lugarONotas = lugarONotas; }
}