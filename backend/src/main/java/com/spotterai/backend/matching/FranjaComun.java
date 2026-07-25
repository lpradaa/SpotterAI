package com.spotterai.backend.matching;

import java.time.LocalTime;

/**
 * Un tramo concreto en el que dos usuarios coinciden.
 *
 * <p>Antes esto era una cadena de texto ("Martes 19:00-21:00"), suficiente para
 * redactar una explicacion pero inutil para dibujar nada. La interfaz necesita el
 * dato estructurado para pintar la rejilla semanal, que es donde se ve de un
 * vistazo cuando podriais entrenar juntos.
 *
 * @param dia        dia en forma canonica, con tilde si le corresponde
 * @param inicio     comienzo del tramo comun
 * @param fin        final del tramo comun
 * @param ambosFijos si los dos declaran ir siempre a esa franja
 */
public record FranjaComun(String dia, LocalTime inicio, LocalTime fin, boolean ambosFijos) {

    /** Duracion del tramo en minutos. */
    public int minutos() {
        return (int) java.time.Duration.between(inicio, fin).toMinutes();
    }

    /** Forma legible, para la explicacion en lenguaje natural. */
    public String describir() {
        String base = "%s %s-%s".formatted(dia, inicio, fin);
        return ambosFijos ? base + " (los dos vais siempre)" : base;
    }
}
