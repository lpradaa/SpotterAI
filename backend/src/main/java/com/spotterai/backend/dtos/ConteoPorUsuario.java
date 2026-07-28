package com.spotterai.backend.dtos;

/** Un numero por persona, para poder contar de todos en una sola consulta. */
public record ConteoPorUsuario(Long usuarioId, long cuantos) {}
