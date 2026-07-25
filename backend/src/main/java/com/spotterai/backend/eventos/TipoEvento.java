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
    SOLICITUD_RESPONDIDA("solicitud-respondida");

    private final String clave;

    TipoEvento(String clave) {
        this.clave = clave;
    }

    public String getClave() {
        return clave;
    }
}
