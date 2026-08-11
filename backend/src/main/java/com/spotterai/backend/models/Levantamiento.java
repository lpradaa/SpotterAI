package com.spotterai.backend.models;

import com.spotterai.backend.matching.Ejercicio;
import jakarta.persistence.*;

/**
 * Una de las marcas principales de alguien, en dato y no en prosa.
 *
 * <p>Convive con {@link Hito} y no lo sustituye, porque son cosas distintas:
 * un hito es lo que alguien quiere contar —con su foto, su fecha y su
 * historia— y esto es su ficha tecnica. Uno se lee, el otro se cruza.
 *
 * <p>El peso es el de la barra completa en kilos. No se normaliza por peso
 * corporal a proposito: para hacer de spotter lo que importa es cuanto pesa lo
 * que hay que sujetar, no cuanto levanta cada uno en relacion a si mismo.
 */
@Entity
@Table(
        name = "levantamiento",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_levantamiento_usuario_ejercicio",
                columnNames = {"usuario_id", "ejercicio"}))
public class Levantamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Ejercicio ejercicio;

    /** Kilos de la barra completa. */
    @Column(nullable = false)
    private Double peso;

    /**
     * Repeticiones a las que se mueve ese peso.
     *
     * Un maximo a una repeticion y un peso de trabajo a diez son cosas muy
     * distintas, y sin este dato no se sabria cual de las dos se esta mirando.
     */
    @Column(nullable = false)
    private Integer repeticiones;

    /** LAZY por lo mismo que en Disponibilidad: solo se le pide el identificador. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Levantamiento() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Ejercicio getEjercicio() { return ejercicio; }
    public void setEjercicio(Ejercicio ejercicio) { this.ejercicio = ejercicio; }
    public Double getPeso() { return peso; }
    public void setPeso(Double peso) { this.peso = peso; }
    public Integer getRepeticiones() { return repeticiones; }
    public void setRepeticiones(Integer repeticiones) { this.repeticiones = repeticiones; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
