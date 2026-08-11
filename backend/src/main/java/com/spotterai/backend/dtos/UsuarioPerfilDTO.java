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

    /**
     * El gimnasio que no estaba en la lista, escrito a mano.
     *
     * <p>El formulario ofrecia "Añadir uno nuevo" desde el principio y mandaba
     * esto, pero el campo no existia aqui: Jackson lo descartaba en silencio y
     * la pantalla decia "Perfil actualizado" sin haber cambiado nada. Quien no
     * encontrara su gimnasio se quedaba sin el para siempre, y no es un dato
     * decorativo: vale 8 puntos y ademas multiplica por 0,25 el solape de
     * horario con quien no lo comparte, o sea que lastra el factor que mas pesa.
     *
     * <p>Manda sobre {@code gimnasioId} si llegan los dos.
     */
    private String nuevoGimnasioNombre;

    /**
     * Existia en la entidad y se enviaba en las tarjetas, pero no habia forma de
     * escribirla: faltaba justo aqui. Era el unico campo libre del perfil, o sea
     * lo unico que distingue a dos personas con el mismo nivel y el mismo
     * objetivo, y estaba muerto.
     */
    private String biografia;

    /**
     * Entrenamientos que se propone hacer a la semana.
     *
     * Vivia en localStorage con la clave meta_semanal_<nombre>: se perdia al
     * cambiar de navegador y dos usuarios con el mismo nombre compartian valor.
     */
    private Integer metaSemanal;

    /**
     * Ruta de la foto de perfil, ya subida a /api/medios.
     *
     * Faltaba aqui, que es el mismo fallo que tenia la biografia: la columna
     * existia, el avatar sabia pintarla y el perfil publico la devolvia, pero
     * sin campo en este DTO cualquier valor que mandara el frontend se
     * descartaba en silencio.
     */
    private String fotoUrl;

    /** Clave del enum Rutina: como reparte la semana. */
    private String rutina;

    /**
     * Si quiere seguir recibiendo avisos por correo.
     *
     * <p>Boolean y no boolean: aqui null significa "no se ha tocado". El perfil
     * se parchea campo a campo —solo se aplica lo que llega— y con un primitivo
     * cualquier guardado parcial que no lo incluyera lo pondria en false y
     * daria de baja a la gente sin que nadie lo pidiera.
     */
    private Boolean avisosPorCorreo;

    /**
     * Hasta tres marcas principales. Null significa "no las toques"; una lista
     * vacia, "borralas todas".
     */
    private List<LevantamientoEntradaDTO> levantamientos;

    /** Lo que llega del formulario: el ejercicio como clave del enum. */
    public static class LevantamientoEntradaDTO {
        private String ejercicio;
        private Double peso;
        private Integer repeticiones;

        public String getEjercicio() { return ejercicio; }
        public void setEjercicio(String ejercicio) { this.ejercicio = ejercicio; }
        public Double getPeso() { return peso; }
        public void setPeso(Double peso) { this.peso = peso; }
        public Integer getRepeticiones() { return repeticiones; }
        public void setRepeticiones(Integer repeticiones) { this.repeticiones = repeticiones; }
    }

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
    public String getNuevoGimnasioNombre() { return nuevoGimnasioNombre; }
    public void setNuevoGimnasioNombre(String nuevoGimnasioNombre) {
        this.nuevoGimnasioNombre = nuevoGimnasioNombre;
    }
    public String getBiografia() { return biografia; }
    public void setBiografia(String biografia) { this.biografia = biografia; }
    public Integer getMetaSemanal() { return metaSemanal; }
    public void setMetaSemanal(Integer metaSemanal) { this.metaSemanal = metaSemanal; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public String getRutina() { return rutina; }
    public void setRutina(String rutina) { this.rutina = rutina; }
    public Boolean getAvisosPorCorreo() { return avisosPorCorreo; }
    public void setAvisosPorCorreo(Boolean avisosPorCorreo) { this.avisosPorCorreo = avisosPorCorreo; }
    public List<LevantamientoEntradaDTO> getLevantamientos() { return levantamientos; }
    public void setLevantamientos(List<LevantamientoEntradaDTO> levantamientos) {
        this.levantamientos = levantamientos;
    }
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