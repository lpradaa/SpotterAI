package com.spotterai.backend.services;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Reduce el nombre de un gimnasio a la forma con la que se comparan dos.
 *
 * <p>Existe porque el gimnasio no es un campo decorativo: vale 8 puntos propios
 * y, sobre todo, multiplica por 0,25 el solape de horario de quien no lo
 * comparte, que es el factor que mas pesa. Dos personas del mismo edificio que
 * hayan escrito "McFit Centro" y "mcfit  centro" no son dos gimnasios, pero sin
 * normalizar el catalogo se parten en dos y el motor las trata como si
 * entrenaran en ciudades distintas.
 *
 * <p>Lo que se guarda sigue siendo lo que la persona escribio. Esto solo se usa
 * para decidir si ya existe: cambiarle el nombre a alguien porque tiene una
 * tilde de mas seria peor que el problema.
 */
public final class NombreDeGimnasio {

    private NombreDeGimnasio() {}

    /**
     * Minusculas, sin tildes y con los espacios de dentro reducidos a uno.
     *
     * <p>Las tildes se quitan a proposito: "Gimnasio Olímpico" y "Gimnasio
     * Olimpico" los escribe la misma gente segun tenga el teclado a mano, y
     * ninguna de las dos formas es la equivocada.
     */
    public static String normalizar(String nombre) {
        if (nombre == null) return "";

        String sinTildes = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return sinTildes.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
