package com.spotterai.backend.dtos;

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

    private Boolean yaConectado = false;
    private Boolean solicitudPendiente = false;

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
}