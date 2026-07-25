package com.spotterai.backend.matching;

/**
 * Un factor individual del calculo de compatibilidad.
 *
 * <p>Cada factor guarda lo que aporto y lo maximo que podia aportar, para que la
 * puntuacion final sea siempre auditable: la suma de los factores es el score.
 *
 * @param nombre     identificador del factor ("horario", "nivel", ...)
 * @param puntos     puntos obtenidos
 * @param puntosMax  puntos maximos que este factor puede aportar
 * @param detalle    explicacion en lenguaje natural de por que se dieron esos puntos
 */
public record FactorCompatibilidad(String nombre, double puntos, double puntosMax, String detalle) {

    /** Proporcion obtenida sobre el maximo, de 0 a 1. */
    public double ratio() {
        return puntosMax == 0 ? 0 : puntos / puntosMax;
    }
}
