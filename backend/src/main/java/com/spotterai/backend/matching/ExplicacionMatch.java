package com.spotterai.backend.matching;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Explicacion en lenguaje natural de por que dos usuarios encajan.
 *
 * <p>Es tambien el esquema de salida estructurada que se le pide a Claude: las
 * anotaciones de Jackson viajan al modelo como descripciones de cada campo, de
 * forma que la respuesta siempre llega con esta forma exacta y no hay que parsear
 * texto libre.
 *
 * @param titular frase corta para la cabecera de la tarjeta
 * @param motivo  dos o tres frases explicando la compatibilidad
 */
public record ExplicacionMatch(

        @JsonPropertyDescription(
                "Titular de una sola frase, maximo 60 caracteres, que resuma el punto fuerte "
                        + "de la compatibilidad. Sin signos de exclamacion.")
        String titular,

        @JsonPropertyDescription(
                "Dos o tres frases dirigidas al usuario en segunda persona del plural, "
                        + "explicando por que encajan como companeros de entrenamiento. "
                        + "Menciona siempre los dias y horas concretas en que coinciden si las hay.")
        String motivo
) {}
