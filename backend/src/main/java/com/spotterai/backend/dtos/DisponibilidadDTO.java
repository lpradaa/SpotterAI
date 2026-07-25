package com.spotterai.backend.dtos;

import java.time.LocalTime;

public class DisponibilidadDTO {
    private Long id;
    private String diaSemana;
    private LocalTime horaInicio; // Cambiado a LocalTime
    private LocalTime horaFin;    // Cambiado a LocalTime

    // Constructores
    public DisponibilidadDTO() {}

    public DisponibilidadDTO(Long id, String diaSemana, LocalTime horaInicio, LocalTime horaFin) {
        this.id = id;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
}