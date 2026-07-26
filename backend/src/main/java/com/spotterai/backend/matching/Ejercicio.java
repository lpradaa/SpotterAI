package com.spotterai.backend.matching;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Los levantamientos que se pueden comparar entre dos personas.
 *
 * <p>Lista cerrada y no texto libre: la gracia de este dato es que el motor
 * pueda cruzarlo, y "banca", "press banca" y "press de banca" escritos a mano
 * son tres ejercicios distintos para una maquina.
 *
 * <p>Solo movimientos con barra donde el peso es directamente comparable. Las
 * dominadas o los fondos se hacen con el peso corporal mas un lastre opcional,
 * asi que "15 kg" en dominadas y "15 kg" en sentadilla no significan nada
 * parecido y meterlos aqui ensuciaria la comparacion. Para eso estan las marcas
 * en texto libre del perfil.
 */
public enum Ejercicio {

    SENTADILLA("Sentadilla"),
    PRESS_BANCA("Press de banca"),
    PESO_MUERTO("Peso muerto"),
    PRESS_MILITAR("Press militar"),
    REMO_BARRA("Remo con barra"),
    HIP_THRUST("Hip thrust");

    private final String nombre;

    Ejercicio(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    /** Vacio si el valor no es uno de los conocidos, en vez de reventar. */
    public static Optional<Ejercicio> desde(String valor) {
        if (valor == null || valor.isBlank()) return Optional.empty();
        String clave = valor.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(e -> e.name().equals(clave)).findFirst();
    }
}
