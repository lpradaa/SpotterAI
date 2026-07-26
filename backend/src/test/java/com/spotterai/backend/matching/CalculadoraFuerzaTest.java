package com.spotterai.backend.matching;

import com.spotterai.backend.models.Levantamiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lo que se protege aqui es que el numero signifique lo que dice significar:
 * si dos personas pueden cubrirse con la barra cargada.
 */
class CalculadoraFuerzaTest {

    private static Levantamiento lev(Ejercicio ejercicio, double peso, int reps) {
        Levantamiento l = new Levantamiento();
        l.setEjercicio(ejercicio);
        l.setPeso(peso);
        l.setRepeticiones(reps);
        return l;
    }

    @Test
    @DisplayName("Mismo peso a las mismas repeticiones es compatibilidad total")
    void mismaFuerza() {
        var c = CalculadoraFuerza.comparar(
                List.of(lev(Ejercicio.PRESS_BANCA, 100, 5)),
                List.of(lev(Ejercicio.PRESS_BANCA, 100, 5)));

        assertTrue(c.hayDatos());
        assertEquals(1.0, c.ratio(), 0.001);
    }

    @Test
    @DisplayName("Las repeticiones cuentan: 100 a 1 no es lo mismo que 100 a 10")
    void lasRepeticionesImportan() {
        // Sin normalizar por repeticiones, estos dos saldrian identicos, y no lo
        // son ni de lejos: quien mueve el mismo peso diez veces es mucho mas fuerte.
        double unaRep = CalculadoraFuerza.maximoEstimado(100, 1);
        double diezReps = CalculadoraFuerza.maximoEstimado(100, 10);

        assertTrue(diezReps > unaRep * 1.25,
                "Diez repeticiones deberian estimar bastante mas maximo que una");
    }

    @Test
    @DisplayName("Una diferencia grande de fuerza puntua muy bajo")
    void diferenciaGrande() {
        // 45 kg no saca a nadie de debajo de una barra de 100.
        var c = CalculadoraFuerza.comparar(
                List.of(lev(Ejercicio.PRESS_BANCA, 100, 5)),
                List.of(lev(Ejercicio.PRESS_BANCA, 45, 5)));

        assertTrue(c.ratio() <= 0.1, "Menos de la mitad de fuerza no puede puntuar alto");
    }

    @Test
    @DisplayName("Una diferencia pequeña sigue siendo buena pareja")
    void diferenciaPequena() {
        var c = CalculadoraFuerza.comparar(
                List.of(lev(Ejercicio.SENTADILLA, 100, 5)),
                List.of(lev(Ejercicio.SENTADILLA, 90, 5)));

        assertEquals(1.0, c.ratio(), 0.001, "Un 10 % de diferencia no cambia nada en la practica");
    }

    @Test
    @DisplayName("Sin ejercicios en comun no hay dato, que no es lo mismo que incompatible")
    void sinEjerciciosEnComun() {
        var c = CalculadoraFuerza.comparar(
                List.of(lev(Ejercicio.SENTADILLA, 120, 5)),
                List.of(lev(Ejercicio.PRESS_MILITAR, 50, 5)));

        // Si esto devolviera 0 en vez de "sin datos", dos personas que registraron
        // ejercicios distintos apareceria como mal encaje sin motivo.
        assertFalse(c.hayDatos());
        assertTrue(c.comunes().isEmpty());
    }

    @Test
    @DisplayName("Se promedian todos los ejercicios que ambos tengan")
    void promedioDeVariosEjercicios() {
        var c = CalculadoraFuerza.comparar(
                List.of(lev(Ejercicio.SENTADILLA, 100, 5), lev(Ejercicio.PRESS_BANCA, 100, 5)),
                List.of(lev(Ejercicio.SENTADILLA, 100, 5), lev(Ejercicio.PRESS_BANCA, 40, 5)));

        assertEquals(2, c.comunes().size());
        // Uno perfecto y otro pesimo: ni excelente ni desastre
        assertTrue(c.ratio() > 0.3 && c.ratio() < 0.8, "Salio " + c.ratio());
    }

    @Test
    @DisplayName("La comparacion da igual en un sentido que en el otro")
    void esSimetrica() {
        var a = List.of(lev(Ejercicio.PESO_MUERTO, 180, 3));
        var b = List.of(lev(Ejercicio.PESO_MUERTO, 120, 3));

        // Una nota que a ti te saliera 80 y a la otra persona 40 seria imposible
        // de explicar en pantalla.
        assertEquals(CalculadoraFuerza.comparar(a, b).ratio(),
                CalculadoraFuerza.comparar(b, a).ratio(), 0.001);
    }

    @Test
    @DisplayName("Un levantamiento incompleto se ignora en vez de romper")
    void datosIncompletos() {
        Levantamiento aMedias = new Levantamiento();
        aMedias.setEjercicio(Ejercicio.SENTADILLA);
        // sin peso ni repeticiones

        var c = CalculadoraFuerza.comparar(
                List.of(aMedias), List.of(lev(Ejercicio.SENTADILLA, 100, 5)));

        assertFalse(c.hayDatos());
    }

    @Test
    @DisplayName("Con el mismo ejercicio repetido se queda con la mejor marca")
    void seQuedaConLaMejorMarca() {
        var c = CalculadoraFuerza.comparar(
                List.of(lev(Ejercicio.SENTADILLA, 60, 5), lev(Ejercicio.SENTADILLA, 120, 5)),
                List.of(lev(Ejercicio.SENTADILLA, 120, 5)));

        assertEquals(1.0, c.ratio(), 0.001);
    }

    @Test
    @DisplayName("Las repeticiones se acotan para que la estimacion no se dispare")
    void topeDeRepeticiones() {
        // Epley se desvia mucho por encima de doce; sin tope, 100 kg a 50
        // repeticiones estimaria un maximo absurdo.
        double treinta = CalculadoraFuerza.maximoEstimado(100, 30);
        double doce = CalculadoraFuerza.maximoEstimado(100, CalculadoraFuerza.MAX_REPETICIONES);

        assertEquals(doce, treinta, 0.001);
    }
}
