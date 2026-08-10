package com.spotterai.backend.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Alguien a quien has bloqueado.
 *
 * <p>Es dirigido a proposito: que tu bloquees a alguien no significa que esa
 * persona te haya bloqueado a ti, y desbloquear tiene que deshacer solo lo tuyo.
 *
 * <p>Los <b>efectos</b>, en cambio, son mutuos: mientras exista esta fila,
 * ninguno de los dos ve al otro ni le puede escribir. Eso no es simetria por
 * comodidad. Si el bloqueado siguiera viendote, notaria que tu has desaparecido
 * de su lista y sabria que le has bloqueado —que es justo lo que convierte un
 * bloqueo en un problema para quien lo usa—.
 */
@Entity
@Table(
        name = "bloqueo",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bloqueo_par",
                columnNames = {"bloqueador_id", "bloqueado_id"}))
public class Bloqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bloqueador_id")
    private Usuario bloqueador;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bloqueado_id")
    private Usuario bloqueado;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    public Bloqueo() {}

    public Bloqueo(Usuario bloqueador, Usuario bloqueado, LocalDateTime creadoEn) {
        this.bloqueador = bloqueador;
        this.bloqueado = bloqueado;
        this.creadoEn = creadoEn;
    }

    public Long getId() { return id; }
    public Usuario getBloqueador() { return bloqueador; }
    public Usuario getBloqueado() { return bloqueado; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
