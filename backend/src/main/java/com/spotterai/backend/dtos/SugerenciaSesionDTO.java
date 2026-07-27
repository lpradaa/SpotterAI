package com.spotterai.backend.dtos;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Lo que el formulario de propuesta trae ya puesto.
 *
 * <p>Sale del solape que el motor ya ha calculado, asi que abrir el formulario
 * es leer una frase ("el lunes 3 de 18:00 a 20:00") en vez de ponerse a cuadrar
 * horarios. Es una sugerencia, no una imposicion: los tres campos se pueden
 * cambiar antes de enviar.
 *
 * @param hayFranjas false cuando no hay ningun tramo comun, que es cuando la
 *                   interfaz tiene que decirlo en vez de sugerir una hora al azar
 */
public record SugerenciaSesionDTO(
        boolean hayFranjas,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean ambosFijos,
        String gimnasioNombre
) {
    /** Cuando no hay nada en comun: que el formulario lo diga y no invente. */
    public static SugerenciaSesionDTO sinFranjas(String gimnasioNombre) {
        return new SugerenciaSesionDTO(false, null, null, null, false, gimnasioNombre);
    }
}
