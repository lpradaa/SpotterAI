package com.spotterai.backend.dtos;

/**
 * Lo que se sabe de ti al iniciar sesion.
 *
 * <p><b>El token ya no viaja aqui.</b> Va en una galleta {@code HttpOnly} que el
 * JavaScript de la pagina no puede leer; devolverlo tambien en el cuerpo dejaria
 * la puerta abierta a que alguien lo guardara en {@code localStorage} y todo el
 * cambio no habria servido de nada.
 *
 * <p>Lo que si viaja es {@code expiraEn}, y no es una credencial: saber cuando
 * se te acaba la sesion no permite iniciar ninguna. Lo necesita el frontend para
 * lo mismo que antes sacaba leyendo el token —no montar la aplicacion entera con
 * una sesion ya muerta y dejarte mirando pantallas vacias sin explicacion—.
 *
 * @param expiraEn milisegundos desde 1970, igual que Date.now() en el navegador
 */
public record AuthResponseDTO(Long id, String email, String nombre, long expiraEn) {
}
