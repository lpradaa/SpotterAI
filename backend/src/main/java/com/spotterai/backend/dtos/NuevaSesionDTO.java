package com.spotterai.backend.dtos;

/**
 * Lo que llega del formulario de propuesta.
 *
 * <p>Las horas viajan como texto ("18:00") y no como {@code LocalTime} porque es
 * lo que produce un {@code <input type="time">} sin ayuda de nadie; convertirlas
 * es cosa del servicio, que es quien puede rechazar una hora imposible con un
 * mensaje que se entienda.
 *
 * @param gimnasioId donde quedar, cuando cada uno entrena en un sitio. Opcional:
 *                   si los dos comparten gimnasio no hay nada que preguntar, y
 *                   si no viene, la sesion se guarda sin sitio como hasta ahora.
 */
public record NuevaSesionDTO(String fecha, String horaInicio, String horaFin, String nota,
                             Long gimnasioId) {}
