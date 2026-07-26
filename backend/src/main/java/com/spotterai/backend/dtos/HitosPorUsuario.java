package com.spotterai.backend.dtos;

/** Cuantos hitos tiene una persona. Proyeccion de consulta, no Object[]. */
public record HitosPorUsuario(Long usuarioId, Long cuantos) {}
