package com.spotterai.backend.models; 

import jakarta.persistence.*;
import java.time.LocalTime;

/**
 * Una franja horaria en la que alguien puede entrenar.
 *
 * <p>La restriccion unica no es cosmetica: {@code CalculadoraSolape} suma los
 * minutos de cada par de franjas que se cruzan, asi que una franja guardada dos
 * veces inflaba directamente las horas en comun que anuncia la aplicacion.
 */
@Entity
@Table(
        name = "Disponibilidad",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_disponibilidad_franja",
                columnNames = {"usuario_id", "dia_semana", "hora_inicio", "hora_fin"}))
public class Disponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String diaSemana; // Ejemplo: "Lunes", "Martes"...

    private LocalTime horaInicio;
    private LocalTime horaFin;

    /**
     * Si el usuario va seguro a esta franja ("Voy siempre") o solo podria ir
     * ("Puedo ir").
     *
     * <p>Es la diferencia entre un compromiso y una posibilidad, y pesa mucho en el
     * emparejamiento: que dos personas coincidan una hora a la que ambas van fijo
     * predice que entrenen juntas mejor que seis horas de disponibilidad vaga.
     *
     * <p>Por defecto es false: quien no marque nada no pierde puntos, simplemente
     * sus franjas cuentan como disponibilidad.
     */
    @Column(nullable = false)
    private boolean habitual = false;

    /**
     * De quien es esta franja.
     *
     * <p>LAZY y no el EAGER que pone JPA por defecto, que aqui costaba caro: el
     * motor carga los horarios de todos los candidatos de golpe con
     * {@code findByUsuarioIdIn} y despues los agrupa por {@code getUsuario().getId()}.
     * Con EAGER, Hibernate materializaba el Usuario entero de cada franja, y
     * como la sesion de esa consulta no es la misma en la que se cargaron los
     * usuarios —{@code open-in-view} esta en false a proposito— salia un SELECT
     * por persona. Medido: 59 consultas identicas en una sola llamada a
     * /api/usuarios/matches.
     *
     * <p>Con LAZY el proxy devuelve el identificador sin ir a la base, que es lo
     * unico que se le pide en todo el proyecto: no hay ni un sitio que lea otro
     * campo del usuario a traves de una disponibilidad.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    public Disponibilidad() {}

    /** Franja de disponibilidad simple, sin compromiso. */
    public Disponibilidad(String diaSemana, LocalTime horaInicio, LocalTime horaFin, Usuario usuario) {
        this(diaSemana, horaInicio, horaFin, usuario, false);
    }

    public Disponibilidad(String diaSemana, LocalTime horaInicio, LocalTime horaFin,
                          Usuario usuario, boolean habitual) {
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.usuario = usuario;
        this.habitual = habitual;
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
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public boolean isHabitual() { return habitual; }
    public void setHabitual(boolean habitual) { this.habitual = habitual; }
}