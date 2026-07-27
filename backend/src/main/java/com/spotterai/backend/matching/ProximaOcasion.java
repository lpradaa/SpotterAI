package com.spotterai.backend.matching;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * La primera fecha real en la que dos personas pueden entrenar juntas.
 *
 * <p>El motor sabe decir "coincidis los lunes de 18:00 a 20:00", que es una
 * regla semanal y no un plan. Esto la baja a un dia del calendario, que es lo
 * unico que se puede proponer: nadie queda "los lunes", se queda el lunes 3.
 *
 * <p>Es lo que rellena el formulario de propuesta. Que salga con un dia y una
 * hora ya puestos, sacados del solape que la aplicacion ya ha calculado, es la
 * diferencia entre proponer y tener que ponerse a cuadrar horarios a mano en el
 * chat, que es exactamente lo que la aplicacion existe para evitar.
 *
 * @param fecha      dia concreto
 * @param inicio     comienzo del tramo comun
 * @param fin        final del tramo comun
 * @param ambosFijos si los dos declaran ir siempre a esa franja
 */
public record ProximaOcasion(LocalDate fecha, LocalTime inicio, LocalTime fin, boolean ambosFijos) {

    /**
     * La mejor ocasion a partir de un momento dado, si es que hay alguna.
     *
     * <p>Se prefiere una franja a la que los dos van siempre aunque caiga mas
     * tarde: es la misma idea que sostiene todo el motor, que el compromiso vale
     * mas que la disponibilidad. Proponer un "quizas" de esta tarde por delante
     * de un fijo del lunes seria empezar por el plan que tiene mas papeletas de
     * caerse. Entre iguales, la mas cercana.
     *
     * <p>Un tramo que ya ha empezado hoy no cuenta: se busca la semana que viene.
     * Proponer una hora que acaba de pasar no es un plan, es un error.
     */
    public static Optional<ProximaOcasion> primera(List<FranjaComun> franjas, LocalDateTime ahora) {
        if (franjas == null || franjas.isEmpty()) return Optional.empty();

        return franjas.stream()
                .map(franja -> desde(franja, ahora))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .min(Comparator.comparing(ProximaOcasion::ambosFijos).reversed()
                        .thenComparing(ProximaOcasion::fecha)
                        .thenComparing(ProximaOcasion::inicio));
    }

    private static Optional<ProximaOcasion> desde(FranjaComun franja, LocalDateTime ahora) {
        DayOfWeek dia = DiasSemana.diaDelCalendario(franja.dia());

        // Un dia que no se reconoce no se puede llevar al calendario. Se ignora
        // en vez de reventar: el resto de franjas siguen sirviendo.
        if (dia == null) return Optional.empty();

        LocalDate fecha = ahora.toLocalDate();
        int saltos = (dia.getValue() - fecha.getDayOfWeek().getValue() + 7) % 7;
        fecha = fecha.plusDays(saltos);

        // Si toca hoy pero la hora ya ha pasado, la proxima es dentro de una semana.
        if (saltos == 0 && !franja.inicio().isAfter(ahora.toLocalTime())) {
            fecha = fecha.plusWeeks(1);
        }

        return Optional.of(new ProximaOcasion(fecha, franja.inicio(), franja.fin(), franja.ambosFijos()));
    }
}
