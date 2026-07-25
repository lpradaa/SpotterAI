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
        List<String> franjas = new ArrayList<>();

        for (Disponibilidad mia : unos) {
            String dia = DiasSemana.clave(mia.getDiaSemana());
            if (dia == null || mia.getHoraInicio() == null || mia.getHoraFin() == null) continue;

            for (Disponibilidad suya : porDiaOtros.getOrDefault(dia, List.of())) {
                if (suya.getHoraInicio() == null || suya.getHoraFin() == null) continue;

                LocalTime inicio = maximo(mia.getHoraInicio(), suya.getHoraInicio());
                LocalTime fin = minimo(mia.getHoraFin(), suya.getHoraFin());

                int minutos = minutosEntre(inicio, fin);
                if (minutos <= 0) continue; // no se tocan

                double confianza = confianzaDe(mia.isHabitual(), suya.isHabitual());

                minutosBrutos += minutos;
                minutosEfectivos += minutos * confianza;
                minutosPorDia.merge(dia, minutos, Integer::sum);

                if (mia.isHabitual() && suya.isHabitual()) {
                    diasAncla.add(dia);
                }

                franjas.add(describir(dia, inicio, fin, mia.isHabitual() && suya.isHabitual()));
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

    private static String describir(String dia, LocalTime inicio, LocalTime fin, boolean ancla) {
        String base = "%s %s-%s".formatted(DiasSemana.desdeClave(dia), inicio, fin);
        return ancla ? base + " (los dos vais siempre)" : base;
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
