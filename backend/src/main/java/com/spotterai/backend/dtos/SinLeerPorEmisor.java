package com.spotterai.backend.dtos;

/**
 * Cuantos mensajes sin leer te ha mandado una persona concreta.
 *
 * Es una proyeccion de consulta y no un {@code Object[]} para que el compilador
 * sepa lo que hay dentro: con el array habia que castear por indice, y cambiar
 * el orden de las columnas del SELECT rompia en tiempo de ejecucion sin que
 * nada avisara antes.
 */
public record SinLeerPorEmisor(Long emisorId, Long cuantos) {}
