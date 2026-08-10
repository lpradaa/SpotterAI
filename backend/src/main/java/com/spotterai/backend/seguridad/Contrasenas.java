package com.spotterai.backend.seguridad;

/**
 * Que se acepta como contraseña.
 *
 * <p>Hasta ahora no se comprobaba nada en ningun sitio: se podia registrar una
 * cuenta con la contraseña {@code a}. Con tres puertas por las que se pone una
 * —el registro, el restablecimiento y el cambio desde el perfil— la regla tiene
 * que vivir en una sola, o acabara siendo distinta en cada una.
 *
 * <h2>Por que solo longitud</h2>
 *
 * <p>Nada de "una mayuscula, un numero y un simbolo". Esas reglas no producen
 * contraseñas mejores: producen {@code Password1!} y un post-it. Lo unico que
 * correlaciona de verdad con la resistencia es la longitud, y es lo unico que se
 * puede exigir sin empujar a la gente a un patron predecible.
 *
 * <p>Doce y no ocho porque aqui no hay segundo factor: si esto cae, se cae la
 * cuenta entera. Y no hay tope por arriba —un gestor de contraseñas genera
 * cuarenta caracteres y no hay ninguna razon para rechazarlos—.
 */
public final class Contrasenas {

    public static final int MINIMO = 12;

    private Contrasenas() {}

    /**
     * @throws IllegalArgumentException con el motivo, para poder enseñarlo tal cual
     */
    public static void exigirQueValga(String contrasena) {
        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }
        if (contrasena.length() < MINIMO) {
            throw new IllegalArgumentException(
                    "La contraseña necesita al menos " + MINIMO + " caracteres.");
        }
    }
}
