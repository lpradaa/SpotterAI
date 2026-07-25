package com.spotterai.backend.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cubre el comportamiento del explicador que no depende de la API: el resumen que
 * se le envia al modelo y la explicacion de respaldo cuando no hay clave.
 */
class ExplicadorCompatibilidadTest {

    /** Sin clave configurada, el explicador nunca intenta llamar a la API. */
    private final ExplicadorCompatibilidad explicador = new ExplicadorCompatibilidad("");

    private static PuntuacionCompatibilidad puntuacionConSolape() {
        return new PuntuacionCompatibilidad(
                88,
                List.of(
                        new FactorCompatibilidad("horario", 36, 40, "Coincidis 4 horas a la semana en Martes y Jueves"),
                        new FactorCompatibilidad("nivel", 20, 20, "Los dos entrenais a nivel intermedio"),
                        new FactorCompatibilidad("objetivo", 20, 20, "Buscais lo mismo: hipertrofia"),
                        new FactorCompatibilidad("gimnasio", 15, 15, "Entrenais en el mismo gimnasio: McFit"),
                        new FactorCompatibilidad("edad", 0, 5, "Os llevais 15 anos")),
                new SolapeHorario(240, List.of("Martes", "Jueves"),
                        List.of("Martes 19:00-21:00", "Jueves 19:00-21:00")));
    }

    @Test
    @DisplayName("Sin clave de API se devuelve la explicacion de respaldo, no una excepcion")
    void respaldoSinClave() {
        ExplicacionMatch e = explicador.explicar("Marta", puntuacionConSolape());

        assertTrue(e.titular().contains("Marta"));
        assertTrue(e.titular().contains("88"));
        assertFalse(e.motivo().isBlank());
    }

    @Test
    @DisplayName("El respaldo solo menciona factores que aportaron puntos")
    void respaldoOmiteFactoresACero() {
        ExplicacionMatch e = explicador.explicar("Marta", puntuacionConSolape());

        assertTrue(e.motivo().contains("Coincidis 4 horas"));
        assertFalse(e.motivo().contains("Os llevais 15 anos")); // aporto 0 puntos
    }

    @Test
    @DisplayName("Sin ningun factor positivo el respaldo sigue diciendo algo util")
    void respaldoSinFactoresPositivos() {
        PuntuacionCompatibilidad cero = new PuntuacionCompatibilidad(
                0,
                List.of(new FactorCompatibilidad("horario", 0, 40, "Vuestros horarios no coinciden")),
                SolapeHorario.NINGUNO);

        ExplicacionMatch e = explicador.explicar("Marta", cero);

        assertFalse(e.motivo().isBlank());
        assertTrue(e.motivo().toLowerCase().contains("no hemos encontrado"));
    }

    @Test
    @DisplayName("El resumen enviado al modelo incluye total, desglose y franjas exactas")
    void resumenParaElModelo() {
        String resumen = explicador.construirResumen("Marta", puntuacionConSolape());

        assertTrue(resumen.contains("Marta"));
        assertTrue(resumen.contains("88 sobre 100"));
        assertTrue(resumen.contains("Compatibilidad excelente"));
        assertTrue(resumen.contains("horario: 36 de 40 puntos"));
        assertTrue(resumen.contains("Martes 19:00-21:00"));
    }

    @Test
    @DisplayName("Si no hay solape el resumen lo dice explicitamente")
    void resumenSinSolape() {
        PuntuacionCompatibilidad sinSolape = new PuntuacionCompatibilidad(
                35,
                List.of(new FactorCompatibilidad("nivel", 20, 20, "Mismo nivel"),
                        new FactorCompatibilidad("gimnasio", 15, 15, "Mismo gimnasio")),
                SolapeHorario.NINGUNO);

        String resumen = explicador.construirResumen("Marta", sinSolape);

        assertTrue(resumen.contains("No hay ninguna franja horaria en comun"));
    }

    @Test
    @DisplayName("La etiqueta cubre todos los tramos de puntuacion")
    void tramosDeEtiqueta() {
        assertEquals("Compatibilidad excelente", etiquetaDe(90));
        assertEquals("Muy compatibles", etiquetaDe(75));
        assertEquals("Buena compatibilidad", etiquetaDe(55));
        assertEquals("Compatibilidad parcial", etiquetaDe(35));
        assertEquals("Poca compatibilidad", etiquetaDe(10));
    }

    private static String etiquetaDe(int total) {
        return new PuntuacionCompatibilidad(total, List.of(), SolapeHorario.NINGUNO).etiqueta();
    }
}
