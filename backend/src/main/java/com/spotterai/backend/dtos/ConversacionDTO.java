package com.spotterai.backend.dtos;

import java.time.LocalDateTime;

/**
 * Una fila de la lista de compañeros, con lo necesario para que diga algo.
 *
 * Antes la lista era solo un nombre: no se sabia si habia mensajes, ni cuales,
 * ni cuando, ni cual era la conversacion viva. Todo lo que hace falta para
 * pintarla viaja aqui, en una sola peticion.
 *
 * @param usuarioId      el interlocutor
 * @param nombre         su nombre
 * @param avatar         color de identidad, si lo tiene
 * @param ultimoMensaje  texto del ultimo mensaje, o null si aun no habeis hablado
 * @param ultimaFecha    cuando fue, para ordenar y mostrar
 * @param mioElUltimo    si lo enviaste tu, para anteponer "Tu:"
 * @param sinLeer        cuantos te ha mandado que no has abierto
 */
public record ConversacionDTO(
        Long usuarioId,
        String nombre,
        String avatar,
        String ultimoMensaje,
        LocalDateTime ultimaFecha,
        boolean mioElUltimo,
        long sinLeer
) {}
