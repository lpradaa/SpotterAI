package com.spotterai.backend.dtos;

public class UsuarioRegistroDTO {
    private String nombre;
    private String email;
    private String password; // La recibimos en limpio desde Angular
    private Integer edad;
    private String genero;
    private Float peso;

    // Getters y Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getEdad() { return edad; }
    public void setEdad(Integer edad) { this.edad = edad; }
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }
    public Float getPeso() { return peso; }
    public void setPeso(Float peso) { this.peso = peso; }
}