package com.spotterai.backend.matching;

/**
 * En que franja cae una puntuacion de compatibilidad.
 *
 * <p>Los cortes estaban duplicados en dos componentes del frontend y en ningun
 * sitio del backend, que es como empiezan las divergencias que este proyecto
 * lleva persiguiendo: dos copias de la misma regla, y una se actualiza.
 *
 * <p>Aqui hacen ademas de eje del embudo. Agrupar por tramo y no por numero
 * exacto no es simplificar: con pocas solicitudes, un embudo con cien casillas
 * de una decima cada una no dice nada, y tres grupos con suficiente gente en
 * cada uno si.
 */
public enum Tramo {

    /** Vale la pena escribirle. */
    ALTA(70),

    /** Encaja en algunas cosas y no en otras. */
    MEDIA(40),

    /** Coincidis en poco. */
    BAJA(0);

    private final int minimo;

    Tramo(int minimo) {
        this.minimo = minimo;
    }

    public int minimo() {
        return minimo;
    }

    public static Tramo de(int puntuacion) {
        if (puntuacion >= ALTA.minimo) return ALTA;
        if (puntuacion >= MEDIA.minimo) return MEDIA;
        return BAJA;
    }
}
