package com.spotterai.backend.dtos;

import java.time.LocalDate;

/**
 * Un hito tal y como se enseña.
 *
 * @param titulo      la marca o el logro, en una linea
 * @param descripcion contexto opcional
 * @param fecha       cuando fue
 * @param medioUrl    ruta de la foto o el video, o null
 * @param medioTipo   "imagen" o "video", para saber que etiqueta pintar
 */
public record HitoDTO(
        Long id,
        String titulo,
        String descripcion,
        LocalDate fecha,
        String medioUrl,
        String medioTipo
) {}
