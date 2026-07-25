package com.spotterai.backend.matching;

/**
 * Explicacion en lenguaje natural de por que dos usuarios encajan.
 *
 * <p>La redacta {@link ExplicadorCompatibilidad} a partir del desglose ya
 * puntuado, y viaja al frontend tal cual.
 *
 * @param titular frase corta para la cabecera de la tarjeta
 * @param motivo  dos o tres frases explicando la compatibilidad
 */
public record ExplicacionMatch(String titular, String motivo) {}
