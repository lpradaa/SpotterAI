package com.spotterai.backend.textos;

/**
 * Un "esto no es tuyo" que se le va a enseñar a alguien.
 *
 * <p>Hermano de {@link ErrorDeNegocio} y por el mismo motivo: la frase acababa
 * pintada tal cual en la pantalla, asi que escrita en español salia en español
 * en una pantalla en ingles. La diferencia es el codigo de estado —403 y no
 * 400— y el frontend lo trata distinto, asi que hacen falta dos tipos.
 *
 * <p>Y hacen falta <b>dos</b> y no uno con un campo: Java no deja heredar de dos
 * sitios a la vez, y cada uno tiene que seguir siendo lo que ya era —
 * {@code IllegalArgumentException} el otro, {@code SecurityException} este—
 * para que los {@code catch} que aun quedan por ahi lo sigan capturando. Sin
 * eso, migrarlas de una en una habria ido cambiando el codigo de estado de las
 * que faltaran.
 *
 * <p>Es distinto de que Spring Security te eche por no tener sesion: eso pasa
 * antes de llegar aqui y no lleva frase. Esto es tener sesion y pedir algo de
 * otra persona.
 */
public class ErrorDePermiso extends SecurityException {

    private final transient Mensaje mensaje;

    public ErrorDePermiso(Mensaje mensaje) {
        // Igual que en ErrorDeNegocio: el mensaje de Java se queda con la clave,
        // que es lo que sale en los registros y localiza el sitio mejor que una
        // frase ya traducida.
        super(mensaje.clave());
        this.mensaje = mensaje;
    }

    /** Atajo para el caso corriente: una clave sin nada dentro. */
    public static ErrorDePermiso de(String clave, Object... args) {
        return new ErrorDePermiso(Mensaje.de(clave, args));
    }

    public Mensaje mensaje() {
        return mensaje;
    }
}
