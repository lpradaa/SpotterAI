package com.spotterai.backend.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cuando dos nombres de gimnasio son el mismo.
 *
 * <p>No es una comparacion cosmetica: el gimnasio vale 8 puntos propios y ademas
 * multiplica por 0,25 el solape de horario de quien no lo comparte, que es el
 * factor que mas pesa del motor. Dos vecinos de sala que escriban el nombre con
 * distinta caja tienen que caer en el mismo sitio o el motor los tratara como si
 * entrenaran en ciudades distintas.
 */
class NombreDeGimnasioTest {

    @Test
    @DisplayName("La caja no distingue dos gimnasios")
    void laCajaNoDistingue() {
        assertEquals(NombreDeGimnasio.normalizar("McFit Centro"),
                     NombreDeGimnasio.normalizar("mcfit centro"));
    }

    @Test
    @DisplayName("Los espacios de sobra tampoco")
    void losEspaciosNoDistinguen() {
        assertEquals(NombreDeGimnasio.normalizar("McFit Centro"),
                     NombreDeGimnasio.normalizar("  McFit   Centro  "));
    }

    @Test
    @DisplayName("Las tildes tampoco: el mismo sitio se escribe de las dos formas")
    void lasTildesNoDistinguen() {
        assertEquals(NombreDeGimnasio.normalizar("Gimnasio Olímpico"),
                     NombreDeGimnasio.normalizar("Gimnasio Olimpico"));
    }

    @Test
    @DisplayName("Dos gimnasios distintos siguen siendo distintos")
    void loQueEsDistintoSigueSiendoloDistinto() {
        assertNotEquals(NombreDeGimnasio.normalizar("McFit Centro"),
                        NombreDeGimnasio.normalizar("McFit Norte"));
    }

    @Test
    @DisplayName("Null y espacios en blanco dan cadena vacia, no explotan")
    void nullYBlancoDanVacio() {
        assertEquals("", NombreDeGimnasio.normalizar(null));
        assertEquals("", NombreDeGimnasio.normalizar("   "));
    }
}
