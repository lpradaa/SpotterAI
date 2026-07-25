package com.spotterai.backend.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DiasSemanaTest {

    @Test
    @DisplayName("Todas las variantes de un dia comparten clave")
    void mismaClaveIndependienteDeTildesYMayusculas() {
        assertEquals("miercoles", DiasSemana.clave("Miércoles"));
        assertEquals("miercoles", DiasSemana.clave("miercoles"));
        assertEquals("miercoles", DiasSemana.clave("MIÉRCOLES"));
        assertEquals("miercoles", DiasSemana.clave("  Miercoles  "));
    }

    @Test
    @DisplayName("La forma canonica lleva la tilde que le corresponde")
    void formaCanonica() {
        assertEquals("Miércoles", DiasSemana.canonico("miercoles"));
        assertEquals("Miércoles", DiasSemana.canonico("MIERCOLES"));
        assertEquals("Sábado", DiasSemana.canonico("sabado"));
        assertEquals("Lunes", DiasSemana.canonico("LUNES"));
    }

    @Test
    @DisplayName("Un valor que no es un dia no se reconoce, pero tampoco se pierde")
    void valorDesconocido() {
        assertNull(DiasSemana.clave("Caturday"));
        assertNull(DiasSemana.clave(""));
        assertNull(DiasSemana.clave(null));
        // canonico devuelve la entrada intacta: mejor conservar el dato del
        // usuario que sustituirlo por null al guardar
        assertEquals("Caturday", DiasSemana.canonico("Caturday"));
    }

    @Test
    @DisplayName("La posicion respeta el orden de la semana")
    void ordenDeLaSemana() {
        assertEquals(0, DiasSemana.posicion("lunes"));
        assertEquals(6, DiasSemana.posicion("domingo"));
        // Los desconocidos van al final en vez de romper la ordenacion
        assertEquals(Integer.MAX_VALUE, DiasSemana.posicion("caturday"));
    }
}
