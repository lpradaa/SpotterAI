package com.spotterai.backend.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "Entrenamiento")
public class Entrenamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha; // Ej: 2026-05-20

    @Column(nullable = false)
    private String tipo; // Ej: "Fuerza", "Cardio", "Crossfit"

    private Integer duracionMinutos; // Ej: 60, 90
    
    private String lugarONotas; // Ej: "Gym Habitual", "Récord en sentadilla"

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public Entrenamiento() {}

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public String getLugarONotas() { return lugarONotas; }
    public void setLugarONotas(String lugarONotas) { this.lugarONotas = lugarONotas; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}