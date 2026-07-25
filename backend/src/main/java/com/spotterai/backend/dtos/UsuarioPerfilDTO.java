package com.spotterai.backend.dtos;

import java.util.List;

public class UsuarioPerfilDTO {
    private String avatar; // 👈 NUEVO
    private Integer edad;
    private String genero;
    private Float peso;
    private String nivel; // Ej: Principiante, Intermedio, Avanzado
    private String objetivos; // Ej: Hipertrofia, Resistencia, Pérdida de peso
    private Long gimnasioId; // El ID del gimnasio que ha elegido del desplegable
    
    private List<HorarioDTO> horarios; // 👈 NUEVO: Lista de horarios que llegarán desde Angular

    // Getters y Setters
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Integer getEdad() { return edad; }
    public void setEdad(Integer edad) { this.edad = edad; }
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }
    public Float getPeso() { return peso; }
    public void setPeso(Float peso) { this.peso = peso; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public String getObjetivos() { return objetivos; }
    public void setObjetivos(String objetivos) { this.objetivos = objetivos; }
    public Long getGimnasioId() { return gimnasioId; }
    public void setGimnasioId(Long gimnasioId) { this.gimnasioId = gimnasioId; }
    public List<HorarioDTO> getHorarios() { return horarios; }
    public void setHorarios(List<HorarioDTO> horarios) { this.horarios = horarios; }

    /** Estructura del JSON de horarios. */
    public static class HorarioDTO {
        private String diaSemana;
        private String horaInicio;
        private String horaFin;

        /**
         * Si el usuario va seguro a esta franja ("Voy siempre") o solo podria ir.
         * Opcional: si no llega, cuenta como disponibilidad sin compromiso.
         */
        private boolean habitual;

        public String getDiaSemana() { return diaSemana; }
        public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }
        public String getHoraInicio() { return horaInicio; }
        public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }
        public String getHoraFin() { return horaFin; }
        public void setHoraFin(String horaFin) { this.horaFin = horaFin; }
        public boolean isHabitual() { return habitual; }
        public void setHabitual(boolean habitual) { this.habitual = habitual; }
    }
}