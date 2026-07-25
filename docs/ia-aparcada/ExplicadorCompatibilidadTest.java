package com.spotterai.backend.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
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
                        FactorCompatibilidad.evaluado("horario", 36, 40,
                                "Coincidís 4 horas a la semana en Martes y Jueves"),
                        FactorCompatibilidad.evaluado("nivel", 20, 20,
                                "Los dos entrenáis a nivel intermedio"),
                        FactorCompatibilidad.evaluado("objetivo", 20, 20,
                                "Buscáis lo mismo: hipertrofia"),
                        FactorCompatibilidad.evaluado("gimnasio", 15, 15,
                                "Entrenáis en el mismo gimnasio: McFit"),
                        FactorCompatibilidad.evaluado("edad", 0, 5,
                                "Os lleváis 15 años")),
                new SolapeHorario(240, 240, 2, List.of("Martes", "Jueves"),
                        List.of(
                                new FranjaComun("Martes", LocalTime.of(19, 0), LocalTime.of(21, 0), true),
                                new FranjaComun("Jueves", LocalTime.of(19, 0), LocalTime.of(21, 0), true))));
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

        assertTrue(e.motivo().contains("Coincidís 4 horas"));
        assertFalse(e.motivo().contains("Os lleváis 15 años")); // aportó 0 puntos
    }

    @Test
    @DisplayName("Sin ningun factor positivo el respaldo sigue diciendo algo util")
    void respaldoSinFactoresPositivos() {
        PuntuacionCompatibilidad cero = new PuntuacionCompatibilidad(
                0,
                List.of(FactorCompatibilidad.evaluado("horario", 0, 40, "Vuestros horarios no coinciden")),
                SolapeHorario.NINGUNO);

        ExplicacionMatch e = explicador.explicar("Marta", cero);

        assertFalse(e.motivo().isBlank());
        assertTrue(e.motivo().toLowerCase().contains("no hemos encontrado"));
    }

    @Test
    @DisplayName("Un factor sin datos no se cuela en la explicacion como si fuera un motivo")
    void respaldoIgnoraLosFactoresSinDatos() {
        PuntuacionCompatibilidad incompleta = new PuntuacionCompatibilidad(
                100,
                List.of(FactorCompatibilidad.evaluado("nivel", 100, 100, "Los dos entrenáis a nivel intermedio"),
                        FactorCompatibilidad.sinDatos("horario", "Faltan los horarios de alguno de los dos perfiles")),
                SolapeHorario.NINGUNO);

        ExplicacionMatch e = explicador.explicar("Marta", incompleta);

        assertTrue(e.motivo().contains("nivel intermedio"));
        assertFalse(e.motivo().contains("Faltan los horarios"));
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
                List.of(FactorCompatibilidad.evaluado("nivel", 20, 20, "Mismo nivel"),
                        FactorCompatibilidad.evaluado("gimnasio", 15, 15, "Mismo gimnasio")),
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
