package com.spotterai.backend.matching;

import com.spotterai.backend.models.Disponibilidad;

import java.text.Normalizer;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Cruza las franjas de disponibilidad de dos usuarios y calcula cuanto tiempo
 * coinciden realmente en el gimnasio.
 *
 * <p>Es la pieza central del emparejamiento: dos personas con el mismo nivel y el
 * mismo objetivo no sirven de nada si nunca coinciden.
 */
public final class CalculadoraSolape {

    /** Orden natural de la semana. Las claves van sin tildes, como las normalizadas. */
    private static final List<String> ORDEN_SEMANA =
            List.of("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo");

    /**
     * Forma con la que cada dia se le muestra al usuario. Se compara sin tildes para
     * que "Miércoles" y "Miercoles" cuenten igual, pero se presenta bien escrito.
     */
    private static final Map<String, String> NOMBRE_VISIBLE = Map.of(
            "lunes", "Lunes",
            "martes", "Martes",
            "miercoles", "Miércoles",
            "jueves", "Jueves",
            "viernes", "Viernes",
            "sabado", "Sábado",
            "domingo", "Domingo");

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
            String dia = normalizarDia(mia.getDiaSemana());
            if (dia == null || mia.getHoraInicio() == null || mia.getHoraFin() == null) continue;

            for (Disponibilidad suya : porDiaOtros.getOrDefault(dia, List.of())) {
                if (suya.getHoraInicio() == null || suya.getHoraFin() == null) continue;

                LocalTime inicio = maximo(mia.getHoraInicio(), suya.getHoraInicio());
                LocalTime fin = minimo(mia.getHoraFin(), suya.getHoraFin());

                int minutos = minutosEntre(inicio, fin);
                if (minutos <= 0) continue; // no se tocan

                minutosTotales += minutos;
                minutosPorDia.merge(dia, minutos, Integer::sum);
                franjas.add("%s %s-%s".formatted(nombreVisible(dia), inicio, fin));
            }
        }

        if (minutosTotales == 0) return SolapeHorario.NINGUNO;

        List<String> dias = minutosPorDia.keySet().stream()
                .sorted(Comparator.comparingInt(ORDEN_SEMANA::indexOf))
                .map(CalculadoraSolape::nombreVisible)
                .toList();

        return new SolapeHorario(minutosTotales, dias, List.copyOf(franjas));
    }

    private static Map<String, List<Disponibilidad>> agruparPorDia(List<Disponibilidad> disponibilidades) {
        Map<String, List<Disponibilidad>> porDia = new LinkedHashMap<>();
        for (Disponibilidad d : disponibilidades) {
            String dia = normalizarDia(d.getDiaSemana());
            if (dia == null) continue;
            porDia.computeIfAbsent(dia, k -> new ArrayList<>()).add(d);
        }
        return porDia;
    }

    /**
     * Deja el dia en minusculas y sin tildes para que "Miércoles", "miercoles" y
     * "MIERCOLES" cuenten como el mismo dia. Devuelve null si no reconoce el valor.
     */
    static String normalizarDia(String dia) {
        if (dia == null || dia.isBlank()) return null;
        String limpio = Normalizer.normalize(dia.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);
        return ORDEN_SEMANA.contains(limpio) ? limpio : null;
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

    /** Devuelve el dia tal y como debe leerse, con tilde si le corresponde. */
    private static String nombreVisible(String diaNormalizado) {
        return NOMBRE_VISIBLE.getOrDefault(diaNormalizado, diaNormalizado);
    }
}
