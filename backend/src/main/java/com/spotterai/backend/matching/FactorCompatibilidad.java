package com.spotterai.backend.matching;

/**
 * Un factor individual del calculo de compatibilidad.
 *
 * <p>Cada factor guarda lo que aporto y lo maximo que podia aportar, para que la
 * puntuacion final sea siempre auditable: la suma de los factores es el score.
 *
 * <p>La distincion importante es entre <strong>no aplicable</strong> y
 * <strong>cero puntos</strong>. Que a alguien le falte el gimnasio en el perfil no
 * significa que entrenéis en sitios distintos: significa que no lo sabemos. Antes
 * se puntuaban igual, con lo que no rellenar un campo penalizaba como si fuera un
 * mal encaje. Un factor no aplicable sale del calculo y reparte su peso entre los
 * demas.
 *
 * @param nombre    identificador del factor ("horario", "nivel", ...)
 * @param puntos    puntos obtenidos, ya sobre el peso efectivo
 * @param puntosMax peso efectivo de este factor tras el reparto
 * @param aplicable si habia datos suficientes en ambos perfiles para evaluarlo
 * @param detalle   explicacion en lenguaje natural
 */
public record FactorCompatibilidad(
        String nombre, double puntos, double puntosMax, boolean aplicable, String detalle) {

    /** Factor evaluado con normalidad. */
    public static FactorCompatibilidad evaluado(String nombre, double puntos, double puntosMax, String detalle) {
        return new FactorCompatibilidad(nombre, puntos, puntosMax, true, detalle);
    }

    /** Factor que no se ha podido evaluar por falta de datos. No resta. */
    public static FactorCompatibilidad sinDatos(String nombre, String detalle) {
        return new FactorCompatibilidad(nombre, 0, 0, false, detalle);
    }

    /** Proporcion obtenida sobre el maximo, de 0 a 1. */
    public double ratio() {
        return puntosMax == 0 ? 0 : puntos / puntosMax;
    }

    /** Copia del factor con el peso reescalado tras repartir el de los no aplicables. */
    FactorCompatibilidad conPeso(double nuevoPeso) {
        return new FactorCompatibilidad(nombre, ratio() * nuevoPeso, nuevoPeso, aplicable, detalle);
    }
}
