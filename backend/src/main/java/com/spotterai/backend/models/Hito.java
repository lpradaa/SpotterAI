package com.spotterai.backend.models;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Una marca o un logro que alguien ha conseguido y quiere enseñar.
 *
 * <p>Es el primer contenido de la aplicacion escrito por una persona y no
 * calculado por el motor. Hasta ahora todo lo que se leia en un perfil ajeno lo
 * habia producido {@code CalculadoraCompatibilidad}: un porcentaje y una frase
 * generada. Eso hace que la aplicacion se lea como una herramienta de analisis,
 * porque literalmente lo era.
 *
 * <p>El medio es opcional y guarda una ruta servida por la propia aplicacion, no
 * el fichero: meter binarios en la base la hincha y complica cualquier copia.
 */
@Entity
@Table(name = "hito")
public class Hito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private LocalDate fecha;

    /** Ruta del medio dentro de la aplicacion, o null si es solo texto. */
    @Column(name = "medio_url", length = 255)
    private String medioUrl;

    /** "imagen" o "video". Decide como se pinta, no hay que adivinarlo por la extension. */
    @Column(name = "medio_tipo", length = 20)
    private String medioTipo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Hito() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getMedioUrl() { return medioUrl; }
    public void setMedioUrl(String medioUrl) { this.medioUrl = medioUrl; }
    public String getMedioTipo() { return medioTipo; }
    public void setMedioTipo(String medioTipo) { this.medioTipo = medioTipo; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
