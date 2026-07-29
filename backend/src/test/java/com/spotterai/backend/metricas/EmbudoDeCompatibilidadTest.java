package com.spotterai.backend.metricas;

import com.spotterai.backend.matching.Tramo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lo que el embudo se niega a decir.
 *
 * <p>La parte facil de una tabla de metricas es sumar. La que importa es que no
 * afirme cosas que sus numeros no sostienen, porque el que la mira —yo— llega
 * con una hipotesis puesta y una tabla que imprime "50 %" sobre cuatro casos la
 * confirma sin merecerlo.
 */
class EmbudoDeCompatibilidadTest {

    private static EmbudoDeCompatibilidad.Fila fila(long enviadas, long aceptadas, long entrenadas) {
        return new EmbudoDeCompatibilidad.Fila(Tramo.ALTA, enviadas, aceptadas, entrenadas);
    }

    @Test
    @DisplayName("Con muestra suficiente, las tasas salen")
    void conMuestraDaTasas() {
        EmbudoDeCompatibilidad.Fila f = fila(40, 30, 10);

        assertTrue(f.hayBastante());
        assertEquals(75, f.tasaAceptacion());
        assertEquals(25, f.tasaEntrenamiento());
    }

    @Test
    @DisplayName("Con pocos casos no hay porcentaje, ni siquiera uno «orientativo»")
    void sinMuestraNoHayTasa() {
        // Cuatro de cada ocho no es "el 50 %": es que respondieron cuatro
        // personas. Devolver 50 aqui es la diferencia entre una herramienta que
        // mide y una que da la razon.
        EmbudoDeCompatibilidad.Fila f = fila(8, 4, 2);

        assertFalse(f.hayBastante());
        assertNull(f.tasaAceptacion());
        assertNull(f.tasaEntrenamiento());
    }

    @Test
    @DisplayName("El umbral es el umbral: justo encima cuenta, justo debajo no")
    void elUmbralNoSeRedondea() {
        assertFalse(fila(EmbudoDeCompatibilidad.MINIMO_PARA_MIRARLO - 1, 1, 1).hayBastante());
        assertTrue(fila(EmbudoDeCompatibilidad.MINIMO_PARA_MIRARLO, 1, 1).hayBastante());
    }

    @Test
    @DisplayName("La tasa de entrenamiento va sobre las enviadas, no sobre las aceptadas")
    void laTasaVaSobreLoQueSalio() {
        // 20 enviadas, 10 aceptadas, 5 entrenadas. Sobre aceptadas daria 50 %, y
        // sonaria mucho mejor de lo que es: la pregunta es que devuelve mandar
        // una solicitud de este tramo, no como se porta el trozo que ya dijo si.
        assertEquals(25, fila(20, 10, 5).tasaEntrenamiento());
    }

    @Test
    @DisplayName("No se comparan tramos si alguno no tiene muestra")
    void noSeComparaConUnTramoVacio() {
        EmbudoDeCompatibilidad conHueco = new EmbudoDeCompatibilidad(
                List.of(fila(40, 30, 10), fila(3, 1, 0)), 0);

        // Es exactamente la comparacion que uno quiere hacer —"mira, arriba
        // convierte mucho mas que abajo"— cuando abajo hay tres casos.
        assertFalse(conHueco.sePuedeComparar());

        assertTrue(new EmbudoDeCompatibilidad(
                List.of(fila(40, 30, 10), fila(25, 5, 1)), 0).sePuedeComparar());
    }

    @Test
    @DisplayName("Un embudo sin nada no se puede comparar consigo mismo")
    void vacioNoSeCompara() {
        assertFalse(new EmbudoDeCompatibilidad(List.of(), 0).sePuedeComparar());
    }

    @Test
    @DisplayName("Cero enviadas no revienta")
    void ceroNoRevienta() {
        EmbudoDeCompatibilidad.Fila f = fila(0, 0, 0);

        assertFalse(f.hayBastante());
        assertNull(f.tasaAceptacion());
    }
}
