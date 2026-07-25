package com.spotterai.backend.dtos;

public class AuthResponseDTO {
    private String token; // Aquí irá el JWT
    private Long id;
    private String email;
    private String nombre;

    public AuthResponseDTO(String token, Long id, String email, String nombre) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.nombre = nombre;
    }

    // Getters y Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}