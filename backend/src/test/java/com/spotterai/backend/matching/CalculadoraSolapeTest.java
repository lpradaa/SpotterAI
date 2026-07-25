package com.spotterai.backend.matching;

import com.spotterai.backend.models.Disponibilidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculadoraSolapeTest {

    private static Disponibilidad franja(String dia, String inicio, String fin) {
        return new Disponibilidad(dia, LocalTime.parse(inicio), LocalTime.parse(fin), null);
    }

    @Test
    @DisplayName("Sin franjas en comun no hay solape")
    void sinCoincidencia() {
        SolapeHorario solape = CalculadoraSolape.calcular(
                List.of(franja("Lunes", "08:00", "10:00")),
                List.of(franja("Martes", "08:00", "10:00")));

        assertFalse(solape.hayCoincidencia());
        assertEquals(0, solape.minutosSemanales());
    }

    @Test
    @DisplayName("Franjas que se tocan parcialmente cuentan solo la interseccion")
    void solapeParcial() {
        SolapeHorario solape = CalculadoraSolape.calcular(
                List.of(franja("Lunes", "18:00", "21:00")),
                List.of(franja("Lunes", "20:00", "23:00")));

        assertEquals(60, solape.minutosSemanales());
        assertEquals(List.of("Lunes"), solape.dias());
        assertEquals(List.of("Lunes 20:00-21:00"), solape.franjas());
    }

    @Test
    @DisplayName("Franjas que solo se tocan en el extremo no cuentan como solape")
    void franjasContiguas() {
        SolapeHorario solape = CalculadoraSolape.calcular(
                List.of(franja("Lunes", "18:00", "20:00")),
                List.of(franja("Lunes", "20:00", "22:00")));

        assertFalse(solape.hayCoincidencia());
    }

    @Test
    @DisplayName("Los dias se comparan sin tildes ni mayusculas")
    void normalizaAcentosYMayusculas() {
        SolapeHorario solape = CalculadoraSolape.calcular(
                List.of(franja("Miércoles", "18:00", "20:00")),
                List.of(franja("MIERCOLES", "18:00", "20:00")));

        assertTrue(solape.hayCoincidencia());
        assertEquals(120, solape.minutosSemanales());
    }

    @Test
    @DisplayName("Los dias se comparan sin tilde pero se muestran con ella")
    void muestraElDiaBienEscrito() {
        SolapeHorario solape = CalculadoraSolape.calcular(
                List.of(franja("Miercoles", "18:00", "20:00"), franja("Sabado", "10:00", "11:00")),
                List.of(franja("Miércoles", "18:00", "20:00"), franja("Sábado", "10:00", "11:00")));

        assertEquals(List.of("Miércoles", "Sábado"), solape.dias());
        assertTrue(solape.franjas().contains("Miércoles 18:00-20:00"));
    }

    @Test
    @DisplayName("Los minutos se suman entre varios dias y se ordenan por semana")
    void sumaVariosDiasEnOrden() {
        SolapeHorario solape = CalculadoraSolape.calcular(
                List.of(franja("Viernes", "18:00", "20:00"), franja("Lunes", "18:00", "19:00")),
                List.of(franja("Viernes", "18:00", "20:00"), franja("Lunes", "18:00", "19:00")));

        assertEquals(180, solape.minutosSemanales());
        assertEquals(List.of("Lunes", "Viernes"), solape.dias());
    }

    @Test
    @DisplayName("Un dia que no existe se ignora en vez de romper el calculo")
    void diaDesconocidoSeIgnora() {
        SolapeHorario solape = CalculadoraSolape.calcular(
                List.of(franja("Caturday", "18:00", "20:00")),
                List.of(franja("Caturday", "18:00", "20:00")));

        assertFalse(solape.hayCoincidencia());
    }

    @Test
    @DisplayName("Una lista vacia o nula no revienta")
    void listasVaciasONulas() {
        assertFalse(CalculadoraSolape.calcular(List.of(), List.of()).hayCoincidencia());
        assertFalse(CalculadoraSolape.calcular(null, List.of()).hayCoincidencia());
        assertFalse(CalculadoraSolape.calcular(List.of(franja("Lunes", "08:00", "09:00")), null)
                .hayCoincidencia());
    }
}
