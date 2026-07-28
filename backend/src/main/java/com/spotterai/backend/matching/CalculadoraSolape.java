package com.spotterai.backend.matching;

import com.spotterai.backend.models.Disponibilidad;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cruza las franjas de disponibilidad de dos usuarios y calcula cuanto tiempo
 * coinciden realmente en el gimnasio.
 *
 * <p>No todo el solape vale igual. Una franja marcada como habitual es un
 * compromiso ("voy siempre"); una normal es solo una posibilidad ("puedo ir").
 * Los minutos se ponderan segun la confianza de cada cruce:
 *
 * <pre>
 *   habitual  x habitual   1,00  los dos van seguro
 *   habitual  x disponible 0,60  uno fijo, el otro puede acercarse
 *   disponible x disponible 0,30  ninguno se compromete
 * </pre>
 *
 * <p>Sin esta distincion, declarar disponibilidad de sobra seria la mejor
 * estrategia posible: solaparias con todo el mundo y el factor dejaria de
 * ordenar nada.
 */
public final class CalculadoraSolape {

    /**
     * Lo que dura, como poco, algo que se pueda llamar entrenar juntos.
     *
     * <p>Sin este minimo, cualquier solape positivo contaba. Alguien libre de
     * 19:55 a 21:00 y tu de 18:00 a 20:00 compartis cinco minutos: la aplicacion
     * lo tomaba como una franja comun, lo pintaba en la rejilla y —si los dos
     * habiais marcado esas franjas como fijas— lo daba por "dia ancla", que
     * garantiza el 75 % del factor horario. Treinta puntos por cinco minutos en
     * los que no da tiempo ni a calentar.
     *
     * <p>Cuarenta y cinco minutos es lo que dura la sesion mas corta que sigue
     * siendo una sesion: calentar, unas series, y los descansos entre ellas.
     * Por debajo de eso no es que coincidais poco, es que no coincidis.
     */
    static final int MINUTOS_MINIMOS_DE_SESION = 45;

    private static final double CONFIANZA_AMBOS_HABITUALES = 1.00;
    private static final double CONFIANZA_UNO_HABITUAL = 0.70;
    private static final double CONFIANZA_NINGUNO_HABITUAL = 0.45;

    private CalculadoraSolape() {}

    public static SolapeHorario calcular(List<Disponibilidad> unos, List<Disponibilidad> otros) {
        if (unos == null || otros == null || unos.isEmpty() || otros.isEmpty()) {
            return SolapeHorario.NINGUNO;
        }

        Map<String, List<Disponibilidad>> porDiaOtros = agruparPorDia(otros);

        int minutosBrutos = 0;
        double minutosEfectivos = 0;
        // LinkedHashMap/Set para conservar el orden en que encontramos los dias
        Map<String, Integer> minutosPorDia = new LinkedHashMap<>();
        Set<String> diasAncla = new LinkedHashSet<>();
        List<FranjaComun> franjas = new ArrayList<>();

        for (Disponibilidad mia : unos) {
            String dia = DiasSemana.clave(mia.getDiaSemana());
            if (dia == null || mia.getHoraInicio() == null || mia.getHoraFin() == null) continue;

            for (Disponibilidad suya : porDiaOtros.getOrDefault(dia, List.of())) {
                if (suya.getHoraInicio() == null || suya.getHoraFin() == null) continue;

                LocalTime inicio = maximo(mia.getHoraInicio(), suya.getHoraInicio());
                LocalTime fin = minimo(mia.getHoraFin(), suya.getHoraFin());

                int minutos = minutosEntre(inicio, fin);

                // Los tramos que no dan para entrenar se descartan enteros: no
                // cuentan minutos, ni dia, ni ancla, ni se dibujan en la rejilla,
                // ni se ofrecen al proponer una sesion. Un cuarto de hora en
                // comun no es una coincidencia pequena, es ninguna.
                if (minutos < MINUTOS_MINIMOS_DE_SESION) continue;

                double confianza = confianzaDe(mia.isHabitual(), suya.isHabitual());

                minutosBrutos += minutos;
                minutosEfectivos += minutos * confianza;
                minutosPorDia.merge(dia, minutos, Integer::sum);

                if (mia.isHabitual() && suya.isHabitual()) {
                    diasAncla.add(dia);
                }

                franjas.add(new FranjaComun(
                        DiasSemana.desdeClave(dia), inicio, fin,
                        mia.isHabitual() && suya.isHabitual()));
            }
        }

        if (minutosBrutos == 0) return SolapeHorario.NINGUNO;

        List<String> dias = minutosPorDia.keySet().stream()
                .sorted(Comparator.comparingInt(DiasSemana::posicion))
                .map(DiasSemana::desdeClave)
                .toList();

        return new SolapeHorario(
                minutosBrutos, minutosEfectivos, diasAncla.size(), dias, List.copyOf(franjas));
    }

    static double confianzaDe(boolean unoHabitual, boolean otroHabitual) {
        if (unoHabitual && otroHabitual) return CONFIANZA_AMBOS_HABITUALES;
        if (unoHabitual || otroHabitual) return CONFIANZA_UNO_HABITUAL;
        return CONFIANZA_NINGUNO_HABITUAL;
    }

    private static Map<String, List<Disponibilidad>> agruparPorDia(List<Disponibilidad> disponibilidades) {
        Map<String, List<Disponibilidad>> porDia = new LinkedHashMap<>();
        for (Disponibilidad d : disponibilidades) {
            String dia = DiasSemana.clave(d.getDiaSemana());
            if (dia == null) continue;
            porDia.computeIfAbsent(dia, k -> new ArrayList<>()).add(d);
        }
        return porDia;
    }

    private static int minutosEntre(LocalTime inicio, LocalTime fin) {
        return (int) java.time.Duration.between(inicio, fin).toMinutes();
    }

    private static LocalTime maximo(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalTime minimo(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }
}
