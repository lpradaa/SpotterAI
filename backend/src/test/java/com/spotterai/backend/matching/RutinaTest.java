package com.spotterai.backend.matching;

import com.spotterai.backend.textos.TextosDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lo que se protege aqui es el orden, no los numeros.
 *
 * Que la misma rutina valga 1.0 y una distinta 0.5 es una decision discutible y
 * puede cambiar; que compartir rutina valga mas que no compartirla, no.
 */
class RutinaTest {

    @Test
    @DisplayName("La misma rutina es el mejor encaje posible")
    void mismaRutina() {
        assertEquals(1.0, Rutina.afinidad(Rutina.TORSO_PIERNA, Rutina.TORSO_PIERNA));
    }

    @Test
    @DisplayName("Da igual quien pregunte: el encaje es el mismo en los dos sentidos")
    void esSimetrica() {
        for (Rutina mia : Rutina.values()) {
            for (Rutina suya : Rutina.values()) {
                assertEquals(Rutina.afinidad(mia, suya), Rutina.afinidad(suya, mia),
                        "%s contra %s no da lo mismo al reves".formatted(mia, suya));
            }
        }
    }

    @Test
    @DisplayName("Compartir rutina encaja mejor que dos divisiones distintas, y eso mejor que ninguna coincidencia")
    void elOrdenDeLosCasos() {
        double igual = Rutina.afinidad(Rutina.TORSO_PIERNA, Rutina.TORSO_PIERNA);
        double libre = Rutina.afinidad(Rutina.TORSO_PIERNA, Rutina.LIBRE);
        double dosDivisiones = Rutina.afinidad(Rutina.TORSO_PIERNA, Rutina.EMPUJE_TIRON_PIERNA);
        double divisionContraCuerpoCompleto = Rutina.afinidad(Rutina.TORSO_PIERNA, Rutina.FULL_BODY);

        assertTrue(igual > libre, "Seguir la misma rutina tiene que valer mas que amoldarse");
        assertTrue(libre > dosDivisiones, "Quien se adapta encaja mejor que quien va a otra cosa fija");
        assertTrue(dosDivisiones > divisionContraCuerpoCompleto,
                "Dos divisiones comparten dias; una division y cuerpo completo, casi ninguno");
    }

    @Test
    @DisplayName("Sin rutina fija los dos, tambien encajan del todo")
    void dosSinRutinaFija() {
        assertEquals(1.0, Rutina.afinidad(Rutina.LIBRE, Rutina.LIBRE));
    }

    @Test
    @DisplayName("Un valor desconocido o vacio no es una rutina, y no revienta")
    void loQueNoEsUnaRutina() {
        assertTrue(Rutina.desde(null).isEmpty());
        assertTrue(Rutina.desde("   ").isEmpty());
        assertTrue(Rutina.desde("ARNOLD_SPLIT").isEmpty());
    }

    @Test
    @DisplayName("La clave se lee venga como venga escrita")
    void aceptaMinusculasYEspacios() {
        assertEquals(Rutina.FULL_BODY, Rutina.desde(" full_body ").orElseThrow());
        assertEquals(Rutina.WEIDER, Rutina.desde("weider").orElseThrow());
    }

    @Test
    @DisplayName("Todas las rutinas se pueden explicar, sin combinaciones sin frase")
    void todasTienenExplicacion() {
        for (Rutina mia : Rutina.values()) {
            for (Rutina suya : Rutina.values()) {
                String frase = TextosDePrueba.nuevo().de(Rutina.describir(mia, suya));
                assertNotNull(frase);
                assertFalse(frase.isBlank(), "%s contra %s se queda sin frase".formatted(mia, suya));
            }
        }
    }
}
