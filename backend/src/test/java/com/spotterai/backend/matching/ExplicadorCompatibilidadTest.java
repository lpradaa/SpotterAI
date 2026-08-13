package com.spotterai.backend.matching;

import com.spotterai.backend.textos.TextosDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo que se comprueba aqui es que la explicacion solo diga cosas que la
 * puntuacion respalda: ni factores que no sumaron, ni huecos de datos disfrazados
 * de motivo.
 */
class ExplicadorCompatibilidadTest {

    private final ExplicadorCompatibilidad explicador = new ExplicadorCompatibilidad(TextosDePrueba.nuevo());

    private static PuntuacionCompatibilidad puntuacionConSolape() {
        return new PuntuacionCompatibilidad(
                88,
                List.of(
                        FactorCompatibilidad.evaluado("horario", 36, 40,
                                TextosDePrueba.literal("Coincidís 4 horas a la semana en Martes y Jueves")),
                        FactorCompatibilidad.evaluado("nivel", 20, 20,
                                TextosDePrueba.literal("Los dos entrenáis a nivel intermedio")),
                        FactorCompatibilidad.evaluado("objetivo", 20, 20,
                                TextosDePrueba.literal("Buscáis lo mismo: hipertrofia")),
                        FactorCompatibilidad.evaluado("gimnasio", 15, 15,
                                TextosDePrueba.literal("Entrenáis en el mismo gimnasio: McFit")),
                        FactorCompatibilidad.evaluado("edad", 0, 5,
                                TextosDePrueba.literal("Os lleváis 15 años"))),
                new SolapeHorario(240, 240, 2, List.of("Martes", "Jueves"),
                        List.of(
                                new FranjaComun("Martes", LocalTime.of(19, 0), LocalTime.of(21, 0), true),
                                new FranjaComun("Jueves", LocalTime.of(19, 0), LocalTime.of(21, 0), true))));
    }

    @Test
    @DisplayName("El titular lleva el nombre y la puntuacion")
    void titularConNombreYPuntuacion() {
        ExplicacionMatch e = explicador.explicar("Marta", puntuacionConSolape());

        assertTrue(e.titular().contains("Marta"));
        assertTrue(e.titular().contains("88"));
        assertFalse(e.motivo().isBlank());
    }

    @Test
    @DisplayName("Solo se mencionan factores que aportaron puntos")
    void omiteFactoresACero() {
        ExplicacionMatch e = explicador.explicar("Marta", puntuacionConSolape());

        assertTrue(e.motivo().contains("Coincidís 4 horas"));
        assertFalse(e.motivo().contains("Os lleváis 15 años")); // aportó 0 puntos
    }

    @Test
    @DisplayName("Sin ningun factor positivo se sigue diciendo algo util")
    void sinFactoresPositivos() {
        PuntuacionCompatibilidad cero = new PuntuacionCompatibilidad(
                0,
                List.of(FactorCompatibilidad.evaluado("horario", 0, 40, TextosDePrueba.literal("Vuestros horarios no coinciden"))),
                SolapeHorario.NINGUNO);

        ExplicacionMatch e = explicador.explicar("Marta", cero);

        assertFalse(e.motivo().isBlank());
        assertTrue(e.motivo().toLowerCase().contains("no hemos encontrado"));
    }

    @Test
    @DisplayName("Un factor sin datos no se cuela en la explicacion como si fuera un motivo")
    void ignoraLosFactoresSinDatos() {
        PuntuacionCompatibilidad incompleta = new PuntuacionCompatibilidad(
                100,
                List.of(FactorCompatibilidad.evaluado("nivel", 100, 100, TextosDePrueba.literal("Los dos entrenáis a nivel intermedio")),
                        FactorCompatibilidad.sinDatos("horario", TextosDePrueba.literal("Faltan los horarios de alguno de los dos perfiles"))),
                SolapeHorario.NINGUNO);

        ExplicacionMatch e = explicador.explicar("Marta", incompleta);

        assertTrue(e.motivo().contains("nivel intermedio"));
        assertFalse(e.motivo().contains("Faltan los horarios"));
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
        return TextosDePrueba.nuevo().de(
                new PuntuacionCompatibilidad(total, List.of(), SolapeHorario.NINGUNO).etiqueta());
    }

    // ===================== El desglose =====================
    // El motivo redactado se queda con lo que suma; el desglose tiene que
    // llevarlo TODO, que es justo lo contrario. Son dos cosas distintas y la
    // tentacion es que la segunda herede los filtros de la primera.

    @Test
    @DisplayName("El desglose lleva los ocho factores, tambien los que no suman")
    void elDesgloseNoFiltraNada() {
        ExplicacionMatch e = explicador.explicar("Marta", puntuacionConSolape());

        assertEquals(5, e.factores().size());
        // "edad" aporto 0 puntos: no sale en el motivo pero si en el desglose,
        // porque un cero explicado es informacion y en el resumen era ruido.
        assertTrue(e.factores().stream().anyMatch(f -> f.nombre().equals("edad")));
    }

    @Test
    @DisplayName("Un factor sin datos viaja marcado, no como un cero")
    void loSinDatosSeDistingueDeUnCero() {
        PuntuacionCompatibilidad incompleta = new PuntuacionCompatibilidad(
                100,
                List.of(FactorCompatibilidad.evaluado("nivel", 100, 100, TextosDePrueba.literal("Los dos entrenáis a nivel intermedio")),
                        FactorCompatibilidad.sinDatos("fuerza", TextosDePrueba.literal("Faltan los levantamientos de alguno de los dos"))),
                SolapeHorario.NINGUNO);

        ExplicacionMatch e = explicador.explicar("Marta", incompleta);

        FactorDelDesglose fuerza = e.factores().stream()
                .filter(f -> f.nombre().equals("fuerza")).findFirst().orElseThrow();

        assertFalse(fuerza.aplicable());
        // Sin puntos y sin maximo: es lo que permite pintarlo como "no lo
        // sabemos" en vez de como una barra vacia, que se leeria como un cero.
        assertEquals(0, fuerza.puntos());
        assertEquals(0, fuerza.puntosMax());
        // Y conserva la frase, que dice "alguno de los dos" sin señalar a nadie:
        // este lado no sabe de quien falta el dato y no lo finge.
        // Sin redactar aqui: un FactorDelDesglose ya sale del explicador con la
        // frase hecha, que es todo el sentido de que exista ese tipo aparte.
        assertTrue(fuerza.detalle().contains("alguno de los dos"));
    }

    @Test
    @DisplayName("Cada factor sale con su nombre de pantalla")
    void cadaFactorTraeSuEtiqueta() {
        ExplicacionMatch e = explicador.explicar("Marta", puntuacionConSolape());

        FactorDelDesglose horario = e.factores().stream()
                .filter(f -> f.nombre().equals("horario")).findFirst().orElseThrow();

        assertEquals("Cuándo entrenáis", horario.etiqueta());
    }

    @Test
    @DisplayName("El total del desglose es el mismo numero que el del titular")
    void elTotalNoSeSeparaDelTitular() {
        ExplicacionMatch e = explicador.explicar("Marta", puntuacionConSolape());

        assertEquals(88, e.total());
        assertTrue(e.titular().contains("88"));
        assertEquals("Compatibilidad excelente", e.etiqueta());
    }

    @Test
    @DisplayName("Los puntos se redondean sin que el desglose se despegue del total")
    void losPuntosVanRedondeados() {
        // Pesos con decimales, que es lo que deja el reparto de los no aplicables
        PuntuacionCompatibilidad conDecimales = new PuntuacionCompatibilidad(
                50,
                List.of(FactorCompatibilidad.evaluado("horario", 28.4, 45.7, TextosDePrueba.literal("Coincidís algo"))),
                SolapeHorario.NINGUNO);

        FactorDelDesglose f = explicador.explicar("Marta", conDecimales).factores().get(0);

        assertEquals(28, f.puntos());
        assertEquals(46, f.puntosMax());
    }
}
