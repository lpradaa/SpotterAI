package com.spotterai.backend.metricas;

/**
 * Dos personas, sin orden.
 *
 * <p>Una sesion guarda quien propuso y quien fue invitado, pero para el embudo
 * da igual quien de los dos empezo: lo que se cruza es la pareja. Se normaliza
 * al construirla para que (3, 7) y (7, 3) sean la misma clave y no cuenten dos
 * veces.
 */
public record ParDeUsuarios(Long unoId, Long otroId) {

    public ParDeUsuarios {
        if (unoId != null && otroId != null && unoId > otroId) {
            Long intercambio = unoId;
            unoId = otroId;
            otroId = intercambio;
        }
    }
}
