package com.spotterai.backend.matching;

import com.spotterai.backend.textos.Mensaje;

/**
 * Un dato que falta en el perfil y lo que cuesta no tenerlo.
 *
 * <p>El nombre y el motivo son claves y no frases: este hueco se pinta en tu
 * propia pagina y el mas caro tambien en el tablero, asi que con el texto
 * escrito aqui dentro las dos pantallas decian "Horario" en español por muy en
 * ingles que estuviera el resto. Quien lo sirve lo redacta con el idioma de la
 * peticion, igual que el resto del motor.
 *
 * @param campo   identificador para que el frontend sepa adonde llevar; no se traduce
 * @param nombre  como se llama en pantalla
 * @param puntos  cuantos de los 100 puntos de compatibilidad quedan fuera de juego
 * @param motivo  por que importa, en una frase
 */
public record HuecoDelPerfil(String campo, Mensaje nombre, int puntos, Mensaje motivo) {}
