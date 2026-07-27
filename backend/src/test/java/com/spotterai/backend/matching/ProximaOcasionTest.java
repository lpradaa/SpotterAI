package com.spotterai.backend.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bajar una regla semanal a una fecha del calendario.
 *
 * Es la pieza que rellena el formulario de propuesta, asi que equivocarse aqui
 * significa proponer un dia que no es, y eso se nota en el primer uso.
 */
class ProximaOcasionTest {

    /** Miercoles 1 de julio de 2026, a las diez de la mañana. */
    private static final LocalDateTime MIERCOLES_10H =
            LocalDateTime.of(2026, 7, 1, 10, 0);

    private static FranjaComun franja(String dia, String inicio, String fin, boolean fijos) {
        return new FranjaComun(dia, LocalTime.parse(inicio), LocalTime.parse(fin), fijos);
    }

    @Test
    @DisplayName("Un dia que aun no ha llegado esta semana cae en esta semana")
    void mismaSemana() {
        ProximaOcasion o = ProximaOcasion.primera(
                List.of(franja("Viernes", "18:00", "20:00", false)), MIERCOLES_10H).orElseThrow();

        assertEquals(LocalDate.of(2026, 7, 3), o.fecha());
        assertEquals(LocalTime.of(18, 0), o.inicio());
    }

    @Test
    @DisplayName("Un dia que ya paso esta semana cae en la siguiente")
    void semanaQueViene() {
        ProximaOcasion o = ProximaOcasion.primera(
                List.of(franja("Lunes", "18:00", "20:00", false)), MIERCOLES_10H).orElseThrow();

        assertEquals(LocalDate.of(2026, 7, 6), o.fecha());
    }

    @Test
    @DisplayName("Hoy vale si la hora todavia no ha llegado")
    void hoyMasTarde() {
        ProximaOcasion o = ProximaOcasion.primera(
                List.of(franja("Miércoles", "18:00", "20:00", false)), MIERCOLES_10H).orElseThrow();

        assertEquals(MIERCOLES_10H.toLocalDate(), o.fecha());
    }

    @Test
    @DisplayName("Una hora que ya ha empezado hoy se va a la semana que viene")
    void hoyPeroYaEmpezada() {
        // Proponer una hora que acaba de pasar no es un plan, es un error.
        ProximaOcasion o = ProximaOcasion.primera(
                List.of(franja("Miércoles", "08:00", "10:00", false)), MIERCOLES_10H).orElseThrow();

        assertEquals(LocalDate.of(2026, 7, 8), o.fecha());
    }

    @Test
    @DisplayName("Una franja fija gana a una antes a la que solo 'podriais' ir")
    void elCompromisoManda() {
        // Misma idea que sostiene todo el motor: proponer el "quizas" de mañana
        // por delante del fijo del lunes es empezar por el plan mas fragil.
        ProximaOcasion o = ProximaOcasion.primera(List.of(
                franja("Jueves", "18:00", "20:00", false),
                franja("Lunes", "18:00", "20:00", true)), MIERCOLES_10H).orElseThrow();

        assertTrue(o.ambosFijos());
        assertEquals(LocalDate.of(2026, 7, 6), o.fecha());
    }

    @Test
    @DisplayName("Entre dos iguales, la mas cercana")
    void aIgualdadLaMasCercana() {
        ProximaOcasion o = ProximaOcasion.primera(List.of(
                franja("Sábado", "10:00", "12:00", true),
                franja("Jueves", "18:00", "20:00", true)), MIERCOLES_10H).orElseThrow();

        assertEquals(LocalDate.of(2026, 7, 2), o.fecha());
    }

    @Test
    @DisplayName("Sin franjas en comun no hay nada que sugerir")
    void sinFranjas() {
        assertTrue(ProximaOcasion.primera(List.of(), MIERCOLES_10H).isEmpty());
        assertTrue(ProximaOcasion.primera(null, MIERCOLES_10H).isEmpty());
    }

    @Test
    @DisplayName("Un dia que no se reconoce se ignora, no revienta")
    void diaIlegible() {
        Optional<ProximaOcasion> o = ProximaOcasion.primera(List.of(
                franja("Cumpleaños", "18:00", "20:00", true),
                franja("Jueves", "18:00", "20:00", false)), MIERCOLES_10H);

        // El dia raro se descarta y el resto sigue sirviendo.
        assertEquals(LocalDate.of(2026, 7, 2), o.orElseThrow().fecha());
    }

    @Test
    @DisplayName("Los dias se leen con tilde o sin ella")
    void tildesIndiferentes() {
        LocalDate conTilde = ProximaOcasion.primera(
                List.of(franja("Sábado", "10:00", "12:00", false)), MIERCOLES_10H).orElseThrow().fecha();
        LocalDate sinTilde = ProximaOcasion.primera(
                List.of(franja("sabado", "10:00", "12:00", false)), MIERCOLES_10H).orElseThrow().fecha();

        assertEquals(conTilde, sinTilde);
    }
}
