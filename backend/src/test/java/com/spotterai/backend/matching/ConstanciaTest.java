package com.spotterai.backend.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El único factor que no sale de lo que alguien dice de sí mismo.
 *
 * <p>Lo que se protege aquí es sobre todo la frontera entre «no se sabe» y «no
 * aparece». Un recién llegado y alguien que lo dejó hace dos meses tienen los
 * mismos cero entrenamientos este mes, y no significan lo mismo: confundirlos
 * castiga a quien acaba de registrarse, que es el peor momento para hacerlo.
 */
class ConstanciaTest {

    @Test
    @DisplayName("Sin historial no se juzga")
    void sinHistorialNoSeJuzga() {
        assertFalse(Constancia.DESCONOCIDA.tieneHistorial());
    }

    @Test
    @DisplayName("Cero entrenamientos con historial sí es una respuesta")
    void dejarloEsUnDato() {
        Constancia dejadoDeIr = new Constancia(0, true);

        // Ha entrenado alguna vez y este mes ninguna: eso no es "no se sabe".
        assertTrue(dejadoDeIr.tieneHistorial());
        assertEquals(0.0, dejadoDeIr.ritmo());
    }

    @Test
    @DisplayName("Tres por semana ya es el máximo: no se premia entrenar más")
    void tresPorSemanaBasta() {
        // Doce en cuatro semanas son tres por semana.
        assertEquals(1.0, new Constancia(12, true).ritmo());

        // El que va todos los días no saca más: el motor mide regularidad, no
        // volumen. Quien entrena seis días no es mejor compañero que quien va
        // tres, solo está más ocupado.
        assertEquals(1.0, new Constancia(24, true).ritmo());
    }

    @Test
    @DisplayName("El ritmo sube con la regularidad")
    void masRegularidadMasRitmo() {
        double flojo = new Constancia(2, true).ritmo();
        double medio = new Constancia(6, true).ritmo();
        double bueno = new Constancia(11, true).ritmo();

        assertTrue(flojo < medio, "%.2f deberia ser menor que %.2f".formatted(flojo, medio));
        assertTrue(medio < bueno, "%.2f deberia ser menor que %.2f".formatted(medio, bueno));
    }

    @Test
    @DisplayName("La pareja vale lo que el menos constante de los dos")
    void mandaElQueMenosAparece() {
        Constancia yoMucho = new Constancia(12, true);
        Constancia elPoco = new Constancia(1, true);

        // De nada sirve que tú vayas cuatro días si el otro va uno: la pareja
        // entrena tan a menudo como el que menos aparece.
        assertEquals(elPoco.ritmo(), Constancia.deLaPareja(yoMucho, elPoco));
        assertEquals(elPoco.ritmo(), Constancia.deLaPareja(elPoco, yoMucho));
    }

    @Test
    @DisplayName("Da igual el orden: la pareja es la misma se mire desde donde se mire")
    void esSimetrica() {
        Constancia a = new Constancia(8, true);
        Constancia b = new Constancia(3, true);

        assertEquals(Constancia.deLaPareja(a, b), Constancia.deLaPareja(b, a));
    }

    @Test
    @DisplayName("Todas las combinaciones tienen una frase, sin huecos")
    void todasSeExplican() {
        for (int mios = 0; mios <= 14; mios++) {
            for (int suyos = 0; suyos <= 14; suyos++) {
                String frase = Constancia.describir(
                        new Constancia(mios, true), new Constancia(suyos, true));
                assertNotNull(frase);
                assertFalse(frase.isBlank(), "%d y %d se quedan sin frase".formatted(mios, suyos));
            }
        }
    }

    @Test
    @DisplayName("La frase habla de la pareja, no del que más entrena")
    void laFraseNoAdorna() {
        String frase = Constancia.describir(new Constancia(12, true), new Constancia(0, true));

        // Uno va tres veces por semana y el otro no va: decir "los dos entrenáis
        // con mucha regularidad" sería quedarse con la mitad buena del dato.
        assertTrue(frase.contains("sin entrenar"), frase);
    }
}
