package com.spotterai.backend.dtos;

/**
 * En que estado esta tu relacion con una persona concreta.
 *
 * <p>Existe para no cargar entidades donde solo hacen falta dos datos. El
 * emparejamiento necesita saber, de cada candidato, si ya sois companeros o si
 * hay algo pendiente; leerlo de las {@code Solicitud} enteras obligaba a
 * materializar el emisor y el receptor de cada una —son {@code @ManyToOne}, o
 * sea EAGER— y con ellos su gimnasio, para acabar usando solo un identificador
 * y una cadena.
 *
 * <p>Mismo motivo y misma forma que {@link ConteoPorUsuario}.
 *
 * @param companeroId la otra persona, sea quien sea quien mando la solicitud
 * @param estado      PENDIENTE, ACEPTADA o RECHAZADA
 */
public record EstadoConCompanero(Long companeroId, String estado) {}
