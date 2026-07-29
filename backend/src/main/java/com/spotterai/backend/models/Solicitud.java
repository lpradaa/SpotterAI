package com.spotterai.backend.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Solicitud de conexion entre dos usuarios.
 *
 * <p>La restriccion unica sobre (emisor, receptor) es la que faltaba: hubo
 * duplicados y se parchearon en Java, con el {@code findFirstBy...} y el
 * {@code HashSet} que en el servicio se llama "la solucion al misterio de los
 * clones". Eso no evitaba el duplicado, solo lo escondia al leerlo.
 *
 * <p>Esa restriccion cubre el mismo par en el mismo sentido. El caso inverso
 * —A pide a B y B pide a A— lo cubre {@code uk_solicitud_pareja}, que vive en
 * {@code V7__solicitud_par_unico.sql} sobre dos columnas generadas con
 * LEAST/GREATEST. No se declara aqui porque esas columnas no son campos de la
 * entidad: las calcula la base y Hibernate no tiene por que saber de ellas.
 *
 * <p>Estuvo un tiempo comprobandose solo en {@code SolicitudServiceImpl}, con la
 * excusa de que expresarlo en la base pedia "una migracion de verdad, no
 * {@code ddl-auto=update}". Con Flyway en su sitio, la excusa se acabo. La
 * comprobacion en Java sigue ahi porque da un mensaje claro en el caso normal;
 * la que para dos peticiones simultaneas es esta.
 */
@Entity
@Table(
        name = "Solicitud",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_solicitud_emisor_receptor",
                columnNames = {"emisor_id", "receptor_id"}))
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String estado; // PENDIENTE, ACEPTADA, RECHAZADA

    private LocalDateTime fechaSolicitud;

    /**
     * La compatibilidad que se veia al mandarla, congelada.
     *
     * <p>No se recalcula nunca. El numero de hoy no dice que viste tu aquel dia:
     * la constancia lo mueve sola —depende de los ultimos 28 dias de cada uno— y
     * cada reajuste de pesos deja los historicos sin poder compararse entre si.
     *
     * <p>Es lo unico que permite preguntar hacia atras si el motor acierta, o
     * sea si de las solicitudes que salieron con mas de 80 % acabaron mas en
     * entrenamientos de verdad. Sin esto, cada peso de la calculadora es una
     * opinion que no se puede contrastar con nada.
     *
     * <p>{@code null} en las anteriores a que esto existiera. Se quedan asi:
     * rellenarlas con la puntuacion de hoy seria inventar el dato.
     */
    private Integer compatibilidad;

    /**
     * Cuando se aviso por correo de esta solicitud, si se aviso.
     *
     * <p>Es lo que impide mandar el mismo aviso dos veces. El barrido corre cada
     * minuto y tiene que sobrevivir a reinicios y a que haya dos instancias, asi
     * que lo que decide si ya se aviso vive en la base y no en memoria.
     */
    private LocalDateTime avisadoEn;

    @ManyToOne
    @JoinColumn(name = "emisor_id")
    private Usuario emisor;

    @ManyToOne
    @JoinColumn(name = "receptor_id")
    private Usuario receptor;

    public Solicitud() {
        this.fechaSolicitud = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public LocalDateTime getAvisadoEn() { return avisadoEn; }
    public void setAvisadoEn(LocalDateTime avisadoEn) { this.avisadoEn = avisadoEn; }
    public Integer getCompatibilidad() { return compatibilidad; }
    public void setCompatibilidad(Integer compatibilidad) { this.compatibilidad = compatibilidad; }
    public Usuario getEmisor() {
        return emisor;
    }

    public void setEmisor(Usuario emisor) {
        this.emisor = emisor;
    }

    public Usuario getReceptor() {
        return receptor;
    }

    public void setReceptor(Usuario receptor) {
        this.receptor = receptor;
    }
}
    

