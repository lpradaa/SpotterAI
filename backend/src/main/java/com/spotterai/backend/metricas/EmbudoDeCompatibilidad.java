package com.spotterai.backend.metricas;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spotterai.backend.matching.Tramo;

import java.util.List;

/**
 * Si la compatibilidad predice algo, o no.
 *
 * <p>Toda la calculadora esta construida sobre una apuesta que hasta ahora no
 * se podia comprobar: que dos personas con un 90 % acaban entrenando juntas mas
 * a menudo que dos con un 50 %. Cada peso —el 40 del horario, el 10 de la
 * constancia— sale de razonar sobre como funciona un gimnasio, no de haber
 * mirado que pasa. Puede que sea verdad y puede que no.
 *
 * <p>Esto lo mira. Por cada tramo: cuantas solicitudes salieron, cuantas se
 * aceptaron y cuantas acabaron en una sesion que los dos confirmaron haber
 * entrenado. Las tres columnas del unico embudo que importa, porque la ultima
 * es la unica que significa que la aplicacion sirvio para algo.
 *
 * <p><b>No concluye por su cuenta.</b> Con cuatro solicitudes en un tramo, que
 * la mitad se acepten no es una tasa del 50 %, es ruido, y una tabla que
 * imprime "50 %" invita a creerselo. Por eso cada fila dice si tiene bastante
 * ({@link #MINIMO_PARA_MIRARLO}) y el porcentaje solo existe cuando lo tiene.
 * El numero mas util que puede dar esta pantalla hoy es "todavia no se sabe".
 */
public record EmbudoDeCompatibilidad(List<Fila> filas, int sinPuntuacion) {

    /**
     * Por debajo de esto no se enseña porcentaje.
     *
     * <p>No sale de ninguna tabla estadistica: es el orden de magnitud a partir
     * del cual una diferencia entre tramos deja de poder explicarse por quien
     * pulso el boton esa semana. Es poco para concluir nada firme y bastante
     * para dejar de ser anecdota.
     */
    public static final int MINIMO_PARA_MIRARLO = 20;

    /**
     * Un tramo de compatibilidad y lo que le paso a la gente que cayo en el.
     *
     * @param enviadas    solicitudes que salieron con una puntuacion de este tramo
     * @param aceptadas   cuantas de esas dijo que si la otra persona
     * @param entrenadas  cuantas acabaron en una sesion confirmada por los dos
     */
    public record Fila(Tramo tramo, long enviadas, long aceptadas, long entrenadas) {

        @JsonProperty
        public boolean hayBastante() {
            return enviadas >= MINIMO_PARA_MIRARLO;
        }

        /** Porcentaje de aceptacion, o {@code null} si la muestra no da para decirlo. */
        @JsonProperty
        public Integer tasaAceptacion() {
            return hayBastante() ? porcentaje(aceptadas, enviadas) : null;
        }

        /**
         * Porcentaje que llego a entrenar de verdad, o {@code null}.
         *
         * <p>Sobre las enviadas y no sobre las aceptadas a proposito: lo que se
         * quiere saber es que devuelve una solicitud de este tramo de punta a
         * punta, no como se comporta el subconjunto que ya dijo que si.
         */
        @JsonProperty
        public Integer tasaEntrenamiento() {
            return hayBastante() ? porcentaje(entrenadas, enviadas) : null;
        }

        private static Integer porcentaje(long parte, long total) {
            return total == 0 ? 0 : Math.toIntExact(Math.round(parte * 100.0 / total));
        }
    }

    /**
     * Si hay bastante en todos los tramos como para comparar unos con otros.
     *
     * <p>Comparar un tramo con muestra contra otro sin ella es la forma mas
     * facil de sacar la conclusion que uno ya traia puesta.
     */
    @JsonProperty
    public boolean sePuedeComparar() {
        return !filas.isEmpty() && filas.stream().allMatch(Fila::hayBastante);
    }
}
