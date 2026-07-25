package com.spotterai.backend.matching;

import com.spotterai.backend.models.Disponibilidad;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cruza las franjas de disponibilidad de dos usuarios y calcula cuanto tiempo
 * coinciden realmente en el gimnasio.
 *
 * <p>Es la pieza central del emparejamiento: dos personas con el mismo nivel y el
 * mismo objetivo no sirven de nada si nunca coinciden.
 *
 * <p>La comparacion de dias se delega en {@link DiasSemana}, que tambien usa el
 * servicio de perfil al guardar, de modo que exista una sola definicion de que
 * significa "el mismo dia".
 */
public final class CalculadoraSolape {

    private CalculadoraSolape() {}

    public static SolapeHorario calcular(List<Disponibilidad> unos, List<Disponibilidad> otros) {
        if (unos == null || otros == null || unos.isEmpty() || otros.isEmpty()) {
            return SolapeHorario.NINGUNO;
        }

        Map<String, List<Disponibilidad>> porDiaOtros = agruparPorDia(otros);

        int minutosTotales = 0;
        // LinkedHashMap para conservar el orden en que vamos encontrando los dias
        Map<String, Integer> minutosPorDia = new LinkedHashMap<>();
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

                minutosTotales += minutos;
                minutosPorDia.merge(dia, minutos, Integer::sum);
                franjas.add("%s %s-%s".formatted(DiasSemana.desdeClave(dia), inicio, fin));
            }
        }

        if (minutosTotales == 0) return SolapeHorario.NINGUNO;

        List<String> dias = minutosPorDia.keySet().stream()
                .sorted(Comparator.comparingInt(DiasSemana::posicion))
                .map(DiasSemana::desdeClave)
                .toList();

        return new SolapeHorario(minutosTotales, dias, List.copyOf(franjas));
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
