package com.spotterai.backend.dtos;

/**
 * Cuerpo de una peticion de envio de mensaje.
 *
 * Existe para no recibir el texto crudo. Antes llegaba como cuerpo suelto y el
 * controlador le quitaba las comillas a mano para deshacer el entrecomillado de
 * JSON, lo que se llevaba por delante tambien las que hubiera escrito la
 * persona: «me dijo "vale"» se guardaba como «me dijo vale».
 */
public record NuevoMensajeDTO(String contenido) {}
