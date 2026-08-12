package com.spotterai.backend.dtos;

/**
 * Una marca principal, tal y como viaja al frontend.
 *
 * @param ejercicio      clave del enum, para que el desplegable sepa cual marcar
 * @param nombre         como se llama en pantalla
 * @param maximoEstimado el maximo a una repeticion segun Epley, redondeado
 */
public record LevantamientoDTO(
        String ejercicio,
        String nombre,
        Double peso,
        Integer repeticiones,
        Integer maximoEstimado
) {}
