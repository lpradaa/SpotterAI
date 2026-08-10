package com.spotterai.backend.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Alguien ha dicho que otra persona se ha portado mal.
 *
 * <p>No tiene restriccion de unicidad como {@link Bloqueo}. Bloquear es un
 * estado —o lo tienes puesto o no— y reportar es un registro de sucesos: la
 * misma pareja puede tener varios reportes, de motivos distintos y en fechas
 * distintas, y cada uno es un hecho aparte que merece quedar.
 *
 * <p>No tiene una entidad "quien lo revisa" ni un estado "resuelto/pendiente"
 * todavia. Con cero usuarios y sin nadie moderando de verdad, construir ese
 * flujo entero seria construir una pantalla para un proceso que no existe. Lo
 * que hace falta primero es que el hecho quede escrito en algun sitio en vez
 * de perderse; el flujo de revision se construye cuando haya alguien
 * revisando.
 */
@Entity
@Table(name = "reporte")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reportador_id")
    private Usuario reportador;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reportado_id")
    private Usuario reportado;

    @Column(nullable = false, length = 40)
    private String motivo;

    /** Lo que quiera añadir quien reporta. Puede quedar vacío: el motivo ya dice algo. */
    @Column(length = 500)
    private String detalle;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    public Reporte() {}

    public Reporte(Usuario reportador, Usuario reportado, String motivo, String detalle, LocalDateTime creadoEn) {
        this.reportador = reportador;
        this.reportado = reportado;
        this.motivo = motivo;
        this.detalle = detalle;
        this.creadoEn = creadoEn;
    }

    public Long getId() { return id; }
    public Usuario getReportador() { return reportador; }
    public Usuario getReportado() { return reportado; }
    public String getMotivo() { return motivo; }
    public String getDetalle() { return detalle; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
