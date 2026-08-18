package com.spotterai.backend.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los ejercicios que se comparan, y cuales van sugeridos.
 *
 * <p>La sugerencia no es una preferencia estetica: el factor de fuerza solo
 * puede comparar cuando las dos personas han apuntado <b>el mismo</b> ejercicio,
 * y con seis a elegir eso pasaba en el 22 % de las parejas. Sugerir los tres
 * basicos lo sube al 30 %, medido en {@code docs/medir-el-motor.md}.
 */
class EjercicioTest {

    @Test
    @DisplayName("Los basicos son los tres que casi todo el mundo hace")
    void losTresBasicos() {
        List<Ejercicio> basicos = java.util.Arrays.stream(Ejercicio.values())
                .filter(Ejercicio::esBasico)
                .toList();

        assertEquals(List.of(Ejercicio.SENTADILLA, Ejercicio.PRESS_BANCA, Ejercicio.PESO_MUERTO),
                basicos);
    }

    /**
     * El orden es la sugerencia: es lo unico que hace el cambio. No se quita
     * ninguno de la lista, porque quien entrene otra cosa la tiene que poder
     * apuntar igual.
     */
    @Test
    @DisplayName("Se sirven con los basicos delante, sin perder ninguno")
    void losBasicosPrimero() {
        List<Ejercicio> orden = Ejercicio.sugeridosPrimero();

        assertEquals(Ejercicio.values().length, orden.size(),
                "Sugerir no es filtrar: tienen que estar los seis");
        assertTrue(orden.stream().limit(3).allMatch(Ejercicio::esBasico),
                "Los tres primeros tienen que ser los basicos, y son: " + orden);
        assertTrue(orden.stream().skip(3).noneMatch(Ejercicio::esBasico),
                "Ningun basico puede quedarse detras: " + orden);
    }

    /**
     * Lo que se guarda es la clave del enum, no su posicion ni su nombre.
     *
     * <p>Reordenar la lista es cambiar una sugerencia de pantalla; si eso
     * moviera el dato guardado, cada persona tendria apuntado otro ejercicio
     * distinto del que puso.
     */
    @Test
    @DisplayName("Reordenar la sugerencia no toca lo que se guarda")
    void elOrdenNoEsElDato() {
        for (Ejercicio e : Ejercicio.sugeridosPrimero()) {
            assertEquals(java.util.Optional.of(e), Ejercicio.desde(e.name()));
        }
    }
}
