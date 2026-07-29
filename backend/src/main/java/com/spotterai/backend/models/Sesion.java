package com.spotterai.backend.models;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Un plan concreto para entrenar juntos: dia, hora y sitio.
 *
 * <p>Es lo que le faltaba a la aplicacion para servir de algo. Hasta aqui todo
 * el trabajo del motor terminaba en un numero y un chat abierto: calculabas que
 * dos personas encajan al 90 % y despues no pasaba nada, porque cuadrar el dia
 * seguia siendo cosa suya. Una sesion es el paso de "encajais" a "el lunes a
 * las seis".
 *
 * <p>No sustituye al {@link Entrenamiento}: la sesion es el plan y el
 * entrenamiento es lo que de verdad hiciste. Por eso el paso de uno a otro lo da
 * cada persona por su cuenta, con {@code confirmadaProponente} y
 * {@code confirmadaInvitado}. Que el otro diga que entrenasteis no es prueba de
 * que tu fueras, y apuntarte un entrenamiento que no hiciste estropea el unico
 * dato honesto que tiene el perfil.
 */
@Entity
@Table(name = "sesion")
public class Sesion {

    /** Esperando respuesta del invitado. */
    public static final String PROPUESTA = "PROPUESTA";
    public static final String ACEPTADA = "ACEPTADA";
    public static final String RECHAZADA = "RECHAZADA";

    /** Cancelada por cualquiera de los dos despues de estar en marcha. */
    public static final String CANCELADA = "CANCELADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "proponente_id")
    private Usuario proponente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "invitado_id")
    private Usuario invitado;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    /**
     * Donde. Puede quedar en blanco: si los dos entrenan en el mismo sitio no
     * hace falta decirlo, y si no lo tienen puesto tampoco se lo vamos a
     * inventar.
     */
    @ManyToOne
    @JoinColumn(name = "gimnasio_id")
    private Gimnasio gimnasio;

    @Column(length = 30, nullable = false)
    private String estado = PROPUESTA;

    /** Recado corto de quien propone: "llevo las bandas", "empiezo por pierna". */
    @Column(length = 140)
    private String nota;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    @Column(name = "respondida_en")
    private LocalDateTime respondidaEn;

    /**
     * Cuando se aviso por correo de esta propuesta, si se aviso.
     *
     * <p>Vive en la base y no en memoria por lo mismo que en Solicitud: el
     * barrido tiene que poder reiniciarse sin que a nadie le lleguen dos correos
     * de la misma propuesta.
     */
    private LocalDateTime avisadoEn;

    @Column(name = "confirmada_proponente", nullable = false)
    private boolean confirmadaProponente = false;

    @Column(name = "confirmada_invitado", nullable = false)
    private boolean confirmadaInvitado = false;

    public Sesion() {}

    // --- Reglas que dependen solo de la propia sesion ---

    /** El momento en que arranca, para compararlo con ahora. */
    public LocalDateTime comienzo() {
        return LocalDateTime.of(fecha, horaInicio);
    }

    /** Sigue esperando respuesta. */
    public boolean estaPendiente() {
        return PROPUESTA.equals(estado);
    }

    /** En marcha: aceptada y todavia por delante. */
    public boolean estaEnMarcha(LocalDateTime ahora) {
        return ACEPTADA.equals(estado) && comienzo().isAfter(ahora);
    }

    /** Aceptada y ya pasada: es la que se puede apuntar como entrenamiento. */
    public boolean yaOcurrio(LocalDateTime ahora) {
        return ACEPTADA.equals(estado) && !comienzo().isAfter(ahora);
    }

    /** Si esta persona participa, sea proponiendo o invitada. */
    public boolean participa(Long usuarioId) {
        return proponente.getId().equals(usuarioId) || invitado.getId().equals(usuarioId);
    }

    /** El otro, visto desde uno de los dos. */
    public Usuario elOtro(Long usuarioId) {
        return proponente.getId().equals(usuarioId) ? invitado : proponente;
    }

    /** Si esta persona ya ha dicho que fue. */
    public boolean confirmadaPor(Long usuarioId) {
        return proponente.getId().equals(usuarioId) ? confirmadaProponente : confirmadaInvitado;
    }

    /** Deja constancia de que esta persona fue. */
    public void confirmarPor(Long usuarioId) {
        if (proponente.getId().equals(usuarioId)) {
            confirmadaProponente = true;
        } else {
            confirmadaInvitado = true;
        }
    }

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getProponente() { return proponente; }
    public void setProponente(Usuario proponente) { this.proponente = proponente; }
    public Usuario getInvitado() { return invitado; }
    public void setInvitado(Usuario invitado) { this.invitado = invitado; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public Gimnasio getGimnasio() { return gimnasio; }
    public void setGimnasio(Gimnasio gimnasio) { this.gimnasio = gimnasio; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }
    public LocalDateTime getRespondidaEn() { return respondidaEn; }
    public LocalDateTime getAvisadoEn() { return avisadoEn; }
    public void setAvisadoEn(LocalDateTime avisadoEn) { this.avisadoEn = avisadoEn; }
    public void setRespondidaEn(LocalDateTime respondidaEn) { this.respondidaEn = respondidaEn; }
    public boolean isConfirmadaProponente() { return confirmadaProponente; }
    public void setConfirmadaProponente(boolean v) { this.confirmadaProponente = v; }
    public boolean isConfirmadaInvitado() { return confirmadaInvitado; }
    public void setConfirmadaInvitado(boolean v) { this.confirmadaInvitado = v; }
}
