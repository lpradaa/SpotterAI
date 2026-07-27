package com.spotterai.backend.dtos;

import java.util.List;

public class UsuarioResponseDTO {
    private Long id;
    private String nombre;
    private String email;
    private Integer edad;
    private String genero;
    private Float peso;
    private String nivel;
    private String objetivos;
    private Long gimnasioId;
    private String biografia;
    private String gimnasioNombre;
    
   
    // 🔥 NUEVO CAMPO: Para enviar el emoji al frontend
    private String avatar;

    /**
     * Foto de perfil, si la tiene.
     *
     * Va aparte del avatar y no en su lugar: el avatar es el color de reserva
     * para cuando no hay foto, y quien pinta decide cual usar. Faltaba aqui, que
     * es el cuarto sitio donde ha pasado lo mismo: sin este campo las fotos
     * existian en la base y no se veian en ninguna lista.
     */
    private String fotoUrl;

    /**
     * Nombre legible de su rutina, o null si no la ha dicho.
     *
     * Ya legible y no la clave del enum: quien pinta la tarjeta no deberia tener
     * que llevar una copia de la traduccion, que es como acaban divergiendo.
     */
    private String rutina;

    private Boolean yaConectado = false;
    private Boolean solicitudPendiente = false;

    // --- Compatibilidad (solo se rellena en los endpoints de match) ---
    private Integer compatibilidad;          // 0-100
    private String etiquetaCompatibilidad;   // "Muy compatibles"
    private String resumenCompatibilidad;    // motivo principal, ya legible
    private List<String> diasEnComun;        // dias con solape horario real
    private Integer minutosEnComun;          // minutos semanales de solape
    private Integer diasFijosEnComun;        // dias a los que ambos van siempre

    /**
     * Si la puntuacion se ha calculado sin algun factor por falta de datos.
     * La interfaz lo usa para avisar en vez de presentar el numero como si lo
     * supieramos todo.
     */
    private Boolean compatibilidadIncompleta;

    /**
     * Tramos concretos en los que coincidis, para poder dibujar la rejilla semanal
     * en vez de limitarse a enumerar los dias.
     */
    private List<FranjaComunDTO> franjasEnComun;

    /** Un tramo compartido, tal y como lo necesita la interfaz para pintarlo. */
    public record FranjaComunDTO(String dia, String inicio, String fin, boolean ambosFijos) {}

    // Constructor básico (usado en el registro)
    public UsuarioResponseDTO(Long id, String nombre, String email, Integer edad, String genero, Float peso) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.edad = edad;
        this.genero = genero;
        this.peso = peso;
    }

    // 🔥 CONSTRUCTOR ACTUALIZADO: Ahora recibe el avatar
    public UsuarioResponseDTO(Long id, String nombre, String email, Integer edad, String genero, Float peso, String nivel, String objetivos, Long gimnasioId, String avatar, String biografia, String gimnasioNombre) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.edad = edad;
        this.genero = genero;
        this.peso = peso;
        this.nivel = nivel;
        this.objetivos = objetivos;
        this.gimnasioId = gimnasioId; 
        this.avatar = avatar; 
        this.biografia = biografia;
        this.gimnasioNombre = gimnasioNombre; 
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
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
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getRutina() { return rutina; }
    public void setRutina(String rutina) { this.rutina = rutina; }
    
    public Boolean getYaConectado() { return yaConectado; }
    public void setYaConectado(Boolean yaConectado) { this.yaConectado = yaConectado; }
    
    public Boolean getSolicitudPendiente() { return solicitudPendiente; }
    public void setSolicitudPendiente(Boolean solicitudPendiente) { this.solicitudPendiente = solicitudPendiente; }

     public String getBiografia() {
        return biografia;
    }

    public void setBiografia(String biografia) {
        this.biografia = biografia;
    }
    
    public String getGimnasioNombre() { return gimnasioNombre; }
    public void setGimnasioNombre(String gimnasioNombre) { this.gimnasioNombre = gimnasioNombre; }

    public Integer getCompatibilidad() { return compatibilidad; }
    public void setCompatibilidad(Integer compatibilidad) { this.compatibilidad = compatibilidad; }

    public String getEtiquetaCompatibilidad() { return etiquetaCompatibilidad; }
    public void setEtiquetaCompatibilidad(String etiquetaCompatibilidad) { this.etiquetaCompatibilidad = etiquetaCompatibilidad; }

    public String getResumenCompatibilidad() { return resumenCompatibilidad; }
    public void setResumenCompatibilidad(String resumenCompatibilidad) { this.resumenCompatibilidad = resumenCompatibilidad; }

    public List<String> getDiasEnComun() { return diasEnComun; }
    public void setDiasEnComun(List<String> diasEnComun) { this.diasEnComun = diasEnComun; }

    public Integer getMinutosEnComun() { return minutosEnComun; }
    public void setMinutosEnComun(Integer minutosEnComun) { this.minutosEnComun = minutosEnComun; }

    public Integer getDiasFijosEnComun() { return diasFijosEnComun; }
    public void setDiasFijosEnComun(Integer diasFijosEnComun) { this.diasFijosEnComun = diasFijosEnComun; }

    public Boolean getCompatibilidadIncompleta() { return compatibilidadIncompleta; }
    public void setCompatibilidadIncompleta(Boolean compatibilidadIncompleta) {
        this.compatibilidadIncompleta = compatibilidadIncompleta;
    }

    public List<FranjaComunDTO> getFranjasEnComun() { return franjasEnComun; }
    public void setFranjasEnComun(List<FranjaComunDTO> franjasEnComun) {
        this.franjasEnComun = franjasEnComun;
    }
}