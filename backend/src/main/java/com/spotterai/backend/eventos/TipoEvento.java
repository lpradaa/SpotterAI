package com.spotterai.backend.eventos;

/**
 * Eventos que el servidor empuja hacia el navegador.
 *
 * La clave es el nombre con el que viaja el evento por SSE y con el que el
 * cliente se suscribe, así que cambiarla rompe el frontend: por eso es
 * explícita y no se deriva del nombre de la constante.
 */
public enum TipoEvento {

    /** Mensaje de chat recibido. Viaja el MensajeDTO completo. */
    MENSAJE("mensaje"),

    /** Alguien te ha enviado una solicitud de conexión. */
    SOLICITUD("solicitud"),

    /** Han aceptado o rechazado una solicitud que enviaste tú. */
    SOLICITUD_RESPONDIDA("solicitud-respondida"),

    /** Un compañero propone quedar un día y una hora. Viaja el SesionDTO. */
    SESION("sesion"),

    /**
     * Han aceptado, rechazado o cancelado una sesión en la que participas.
     *
     * Uno solo para los tres casos: el DTO ya trae el estado, y quien lo recibe
     * hace lo mismo con los tres —volver a pintar la sesión tal y como esté—.
     */
    SESION_RESPONDIDA("sesion-respondida"),

    /**
     * Alguien ha retirado su solicitud o ha dejado de ser compañero tuyo.
     *
     * Hace falta avisar porque si no la otra persona sigue viendo un chat que ya
     * no existe, y el primer mensaje que mande se lleva un 403 sin explicacion.
     */
    RELACION_DESHECHA("relacion-deshecha");

    private final String clave;

    TipoEvento(String clave) {
        this.clave = clave;
    }

    public String getClave() {
        return clave;
    }
}
