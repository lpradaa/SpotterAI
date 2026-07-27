package com.spotterai.backend.matching;

import java.util.List;

/**
 * Resultado de cruzar los horarios de dos usuarios.
 *
 * <p>Distingue los minutos brutos de los efectivos porque no todo el solape vale
 * igual: coincidir en una franja a la que ambos van seguro no es lo mismo que
 * coincidir en una a la que ambos "podrian" ir.
 *
 * @param minutosSemanales minutos brutos en comun a lo largo de la semana
 * @param minutosEfectivos los mismos minutos ponderados por la confianza de cada
 *                         cruce; es el numero que se usa para puntuar
 * @param diasAncla        dias en los que <em>ambos</em> declaran ir siempre. Es la
 *                         señal mas fiable de toda la aplicacion
 * @param dias             dias con alguna coincidencia, en orden natural
 * @param franjas          tramos concretos en comun, para poder dibujarlos
 */
public record SolapeHorario(
        int minutosSemanales,
        double minutosEfectivos,
        int diasAncla,
        List<String> dias,
        List<FranjaComun> franjas) {

    public static final SolapeHorario NINGUNO =
            new SolapeHorario(0, 0, 0, List.of(), List.of());

    public boolean hayCoincidencia() {
        return minutosSemanales > 0;
    }

    /**
     * Fiabilidad media del solape, entre 0 y 1.
     *
     * <p>Es la proporcion de los minutos que sobrevive tras ponderar por compromiso.
     * Sirve de techo del factor horario: por mucho rango que declares, no puedes
     * puntuar por encima de lo fiable que es tu propia disponibilidad. Es lo que
     * evita que inflar el horario sea la mejor estrategia.
     */
    public double fiabilidad() {
        return minutosSemanales == 0 ? 0 : minutosEfectivos / minutosSemanales;
    }

    /** Si hay al menos un dia en el que los dos van fijo. */
    public boolean hayAncla() {
        return diasAncla > 0;
    }

    /**
     * Cuales son esos dias, no cuantos.
     *
     * <p>Hacia falta porque la explicacion se contradecia a si misma: decia "los
     * dos vais siempre 2 dias" —que son las anclas— y a continuacion enumeraba
     * los tres dias con algun solape. El numero y la lista hablaban de cosas
     * distintas.
     *
     * <p>Sale de {@code franjas} y no de una columna aparte para que no puedan
     * discrepar, y respeta el orden natural de {@code dias}.
     */
    public List<String> diasDeAncla() {
        return dias.stream()
                .filter(dia -> franjas.stream()
                        .anyMatch(f -> f.ambosFijos() && f.dia().equals(dia)))
                .toList();
    }
}
