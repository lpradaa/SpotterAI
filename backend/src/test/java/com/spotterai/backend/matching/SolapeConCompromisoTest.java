package com.spotterai.backend.matching;

import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.models.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comportamiento de los dos niveles de compromiso horario: "Voy siempre" frente a
 * "Puedo ir".
 */
class SolapeConCompromisoTest {

    private static Disponibilidad puedo(String dia, String inicio, String fin) {
        return new Disponibilidad(dia, LocalTime.parse(inicio), LocalTime.parse(fin), null, false);
    }

    private static Disponibilidad voySiempre(String dia, String inicio, String fin) {
        return new Disponibilidad(dia, LocalTime.parse(inicio), LocalTime.parse(fin), null, true);
    }

    private static Usuario usuarioTipo() {
        Gimnasio g = new Gimnasio();
        g.setId(1L);
        g.setNombre("McFit");

        Usuario u = new Usuario();
        u.setNombre("Test");
        u.setNivel("Intermedio");
        u.setObjetivos("Hipertrofia");
        u.setEdad(30);
        u.setGimnasio(g);
        return u;
    }

    private static double puntosHorario(List<Disponibilidad> mios, List<Disponibilidad> suyos) {
        return factorHorario(mios, suyos).puntos();
    }

    /**
     * Que fraccion de su propio maximo saca el factor horario.
     *
     * Sirve para las aserciones sobre techos: los puntos absolutos suben cuando
     * otro factor se queda sin datos y reparte su peso, asi que compararlos
     * contra una constante rompe el test sin que nada se haya roto de verdad.
     */
    private static double fraccionHorario(List<Disponibilidad> mios, List<Disponibilidad> suyos) {
        var f = factorHorario(mios, suyos);
        return f.puntos() / f.puntosMax();
    }

    private static FactorCompatibilidad factorHorario(
            List<Disponibilidad> mios, List<Disponibilidad> suyos) {
        return CalculadoraCompatibilidad.calcular(usuarioTipo(), mios, usuarioTipo(), suyos)
                .factores().stream()
                .filter(f -> f.nombre().equals("horario"))
                .findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Los mismos minutos valen mas si ambos van siempre")
    void elCompromisoPesaMasQueLaDisponibilidad() {
        List<Disponibilidad> firme = List.of(voySiempre("Martes", "19:00", "21:00"));
        List<Disponibilidad> flojo = List.of(puedo("Martes", "19:00", "21:00"));

        double ambosFirmes = puntosHorario(firme, firme);
        double unoFirme = puntosHorario(firme, flojo);
        double ningunoFirme = puntosHorario(flojo, flojo);

        assertTrue(ambosFirmes > unoFirme,
                "Dos compromisos deben valer mas que uno solo");
        assertTrue(unoFirme > ningunoFirme,
                "Un compromiso debe valer mas que ninguno");
    }

    @Test
    @DisplayName("Una hora a la que los dos van fijo gana a seis horas de disponibilidad vaga")
    void elAnclaGanaAlVolumen() {
        // Este es el motivo de que las anclas pesen mas que los minutos: coincidir
        // poco pero seguro predice mejor que coincidir mucho sin comprometerse.
        List<Disponibilidad> ancla = List.of(voySiempre("Martes", "19:00", "20:00"));

        List<Disponibilidad> vago = List.of(
                puedo("Lunes", "17:00", "19:00"),
                puedo("Miércoles", "17:00", "19:00"),
                puedo("Viernes", "17:00", "19:00"));

        double conAncla = puntosHorario(ancla, ancla);
        double conVolumen = puntosHorario(vago, vago);

        assertTrue(conAncla > conVolumen,
                "1 h de compromiso mutuo (%.1f) deberia superar 6 h vagas (%.1f)"
                        .formatted(conAncla, conVolumen));
    }

    @Test
    @DisplayName("Inflar la disponibilidad sube la puntuacion, pero con techo")
    void ampliarElRangoTieneRendimientosDecrecientes() {
        List<Disponibilidad> escaso = List.of(puedo("Lunes", "18:00", "20:00"));

        // Toda la semana de la mañana a la noche: la estrategia de quien intente inflar
        List<Disponibilidad> exagerado = List.of(
                puedo("Lunes", "07:00", "23:00"), puedo("Martes", "07:00", "23:00"),
                puedo("Miércoles", "07:00", "23:00"), puedo("Jueves", "07:00", "23:00"),
                puedo("Viernes", "07:00", "23:00"), puedo("Sábado", "07:00", "23:00"),
                puedo("Domingo", "07:00", "23:00"));

        double conEscaso = puntosHorario(escaso, escaso);
        double conExagerado = puntosHorario(exagerado, exagerado);

        // Mas disponibilidad real ayuda...
        assertTrue(conExagerado > conEscaso);

        // ...pero el techo es la fiabilidad del propio solape. Sin compromiso no se
        // pasa de ese 45 %, por muchas horas que se declaren: si no, decir "puedo a
        // cualquier hora" seria la mejor jugada posible.
        //
        // Se mide la fraccion del factor y no sus puntos absolutos: al repartirse
        // el peso de los factores ausentes, el horario recibe una parte extra y el
        // numero absoluto sube sin que la regla se haya roto. Esto lo aprendimos
        // al añadir el factor de fuerza, que puso en rojo esta asercion sin que el
        // comportamiento que vigila hubiera cambiado.
        assertTrue(fraccionHorario(exagerado, exagerado) <= 0.46,
                "Sin compromiso no deberia pasar del techo, pero saco %.2f"
                        .formatted(fraccionHorario(exagerado, exagerado)));
    }

    @Test
    @DisplayName("Una sola ancla supera a una semana entera de disponibilidad declarada")
    void elCompromisoSuperaAlRangoInflado() {
        List<Disponibilidad> unaAncla = List.of(voySiempre("Martes", "19:00", "20:00"));

        List<Disponibilidad> semanaEntera = List.of(
                puedo("Lunes", "07:00", "23:00"), puedo("Martes", "07:00", "23:00"),
                puedo("Miércoles", "07:00", "23:00"), puedo("Jueves", "07:00", "23:00"),
                puedo("Viernes", "07:00", "23:00"));

        assertTrue(puntosHorario(unaAncla, unaAncla) > puntosHorario(semanaEntera, semanaEntera),
                "Una hora de compromiso mutuo debe ganar a 80 h de disponibilidad vaga");
    }

    @Test
    @DisplayName("Dos anclas semanales acercan el factor horario a su maximo")
    void dosAnclasCasiSaturanElFactor() {
        List<Disponibilidad> dosAnclas = List.of(
                voySiempre("Martes", "19:00", "21:00"),
                voySiempre("Jueves", "19:00", "21:00"));

        assertTrue(puntosHorario(dosAnclas, dosAnclas) > 40 * 0.85);
    }

    @Test
    @DisplayName("El solape registra en cuantos dias coinciden los compromisos")
    void cuentaLosDiasAncla() {
        List<Disponibilidad> mios = List.of(
                voySiempre("Lunes", "18:00", "20:00"),
                puedo("Martes", "18:00", "20:00"));
        List<Disponibilidad> suyos = List.of(
                voySiempre("Lunes", "18:00", "20:00"),
                voySiempre("Martes", "18:00", "20:00"));

        SolapeHorario solape = CalculadoraSolape.calcular(mios, suyos);

        assertTrue(solape.hayAncla());
        assertEquals(1, solape.diasAncla(), "Solo el lunes es compromiso por ambas partes");
        assertEquals(240, solape.minutosSemanales());
        // 120 min x 1,00 (lunes, ambos fijos) + 120 min x 0,70 (martes, solo uno)
        assertEquals(204.0, solape.minutosEfectivos(), 0.01);
        assertEquals(204.0 / 240, solape.fiabilidad(), 0.01);
    }

    @Test
    @DisplayName("Sin marcar nada, todo cuenta como disponibilidad y no se penaliza")
    void porDefectoNoHayCompromisoYNoPasaNada() {
        List<Disponibilidad> sinMarcar = List.of(puedo("Lunes", "18:00", "20:00"));
        SolapeHorario solape = CalculadoraSolape.calcular(sinMarcar, sinMarcar);

        assertTrue(solape.hayCoincidencia());
        assertFalse(solape.hayAncla());
        assertEquals(0, solape.diasAncla());
        assertTrue(puntosHorario(sinMarcar, sinMarcar) > 0);
    }
}
