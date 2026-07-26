package com.spotterai.backend.matching;

/**
 * Un dato que falta en el perfil y lo que cuesta no tenerlo.
 *
 * @param campo   identificador para que el frontend sepa adonde llevar
 * @param nombre  como se llama en pantalla
 * @param puntos  cuantos de los 100 puntos de compatibilidad quedan fuera de juego
 * @param motivo  por que importa, en una frase
 */
public record HuecoDelPerfil(String campo, String nombre, int puntos, String motivo) {}
