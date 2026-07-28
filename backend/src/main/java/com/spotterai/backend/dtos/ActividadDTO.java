package com.spotterai.backend.dtos;

import java.time.LocalDate;

/**
 * Algo que ha hecho alguien con quien entrenas.
 *
 * <p>Es la única concesión «red social» de la aplicación, y va acotada a
 * propósito: solo de la gente con la que ya has conectado. Un muro abierto
 * invita a publicar para que lo vean, y de ahí a los contadores de vanidad hay
 * un paso; esto responde a otra pregunta, la de «¿qué está haciendo la gente con
 * la que entreno?», que es legítima y no necesita público.
 *
 * <p>No lleva ni «me gusta» ni comentarios, y no es un descuido: nada de eso
 * ayuda a que dos personas coincidan un martes, que es la vara con la que se ha
 * medido todo lo demás.
 *
 * @param tipo      HITO o ENTRENAMIENTO. Se pintan distinto porque son cosas
 *                  distintas: uno es un logro que alguien decide contar, el otro
 *                  una sesión registrada
 * @param titulo    la marca, o el tipo de entrenamiento
 * @param detalle   contexto opcional: la descripción del hito o el lugar
 * @param medioUrl  foto o vídeo del hito, si lo lleva
 */
public record ActividadDTO(
        String tipo,
        Long usuarioId,
        String usuarioNombre,
        String usuarioAvatar,
        String usuarioFotoUrl,
        String titulo,
        String detalle,
        LocalDate fecha,
        String medioUrl,
        String medioTipo
) {
    public static final String HITO = "HITO";
    public static final String ENTRENAMIENTO = "ENTRENAMIENTO";
}
