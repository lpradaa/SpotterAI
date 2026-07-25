package com.spotterai.backend.matching;

import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.models.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculadoraCompatibilidadTest {

    private static Gimnasio gimnasio(long id, String nombre) {
        Gimnasio g = new Gimnasio();
        g.setId(id);
        g.setNombre(nombre);
        return g;
    }

    private static Usuario usuario(String nivel, String objetivo, Integer edad, Gimnasio gimnasio) {
        Usuario u = new Usuario();
        u.setNombre("Test");
        u.setNivel(nivel);
        u.setObjetivos(objetivo);
        u.setEdad(edad);
        u.setGimnasio(gimnasio);
        return u;
    }

    /** Franja sin compromiso: "puedo ir". */
    private static Disponibilidad franja(String dia, String inicio, String fin) {
        return new Disponibilidad(dia, LocalTime.parse(inicio), LocalTime.parse(fin), null, false);
    }

    /** Franja con compromiso: "voy siempre". */
    private static Disponibilidad fija(String dia, String inicio, String fin) {
        return new Disponibilidad(dia, LocalTime.parse(inicio), LocalTime.parse(fin), null, true);
    }

    private static double puntosDe(PuntuacionCompatibilidad p, String factor) {
        return p.factores().stream()
                .filter(f -> f.nombre().equals(factor))
                .findFirst().orElseThrow().puntos();
    }

    @Test
    @DisplayName("La compatibilidad maxima exige compromiso, no solo disponibilidad")
    void compatibilidadPerfecta() {
        Gimnasio g = gimnasio(1L, "McFit Centro");
        Usuario a = usuario("Intermedio", "Hipertrofia", 28, g);
        Usuario b = usuario("Intermedio", "Hipertrofia", 29, g);

        // Las tres franjas con compromiso mutuo: el maximo del tope permitido
        List<Disponibilidad> conCompromiso = List.of(
                fija("Lunes", "18:00", "20:00"),
                fija("Miercoles", "18:00", "20:00"),
                fija("Viernes", "18:00", "20:00"));

        assertEquals(100, CalculadoraCompatibilidad.calcular(a, conCompromiso, b, conCompromiso).total());

        // Basta con que una de las tres sea solo disponibilidad para no llegar al tope
        List<Disponibilidad> casiTodoFirme = List.of(
                fija("Lunes", "18:00", "20:00"),
                fija("Miercoles", "18:00", "20:00"),
                franja("Viernes", "18:00", "20:00"));

        assertTrue(CalculadoraCompatibilidad.calcular(a, casiTodoFirme, b, casiTodoFirme).total() < 100);

        // Las mismas horas, pero sin que ninguno se comprometa, no llegan al maximo:
        // es disponibilidad, no una cita.
        List<Disponibilidad> soloDisponibles = List.of(
                franja("Lunes", "18:00", "20:00"),
                franja("Miercoles", "18:00", "20:00"),
                franja("Viernes", "18:00", "20:00"));

        PuntuacionCompatibilidad vaga =
                CalculadoraCompatibilidad.calcular(a, soloDisponibles, b, soloDisponibles);
        assertTrue(vaga.total() < 100);
        assertTrue(vaga.total() > 60, "Aun asi debe puntuar bien: %d".formatted(vaga.total()));
    }

    @Test
    @DisplayName("Sin solape horario se pierden los 40 puntos aunque el resto encaje")
    void sinSolapeHorarioPierde40() {
        Gimnasio g = gimnasio(1L, "McFit Centro");
        Usuario a = usuario("Intermedio", "Hipertrofia", 28, g);
        Usuario b = usuario("Intermedio", "Hipertrofia", 28, g);

        PuntuacionCompatibilidad p = CalculadoraCompatibilidad.calcular(
                a, List.of(franja("Lunes", "07:00", "09:00")),
                b, List.of(franja("Lunes", "20:00", "22:00")));

        assertEquals(0, puntosDe(p, "horario"));
        assertEquals(60, p.total()); // 20 nivel + 20 objetivo + 15 gimnasio + 5 edad
    }

    @Test
    @DisplayName("Niveles contiguos puntuan la mitad; extremos puntuan cero")
    void escalaDeNivel() {
        Gimnasio g = gimnasio(1L, "Gym");
        List<Disponibilidad> h = List.of(franja("Lunes", "18:00", "19:00"));

        PuntuacionCompatibilidad contiguos = CalculadoraCompatibilidad.calcular(
                usuario("Principiante", "Fuerza", 30, g), h,
                usuario("Intermedio", "Fuerza", 30, g), h);
        assertEquals(10, puntosDe(contiguos, "nivel"));

        PuntuacionCompatibilidad extremos = CalculadoraCompatibilidad.calcular(
                usuario("Principiante", "Fuerza", 30, g), h,
                usuario("Avanzado", "Fuerza", 30, g), h);
        assertEquals(0, puntosDe(extremos, "nivel"));
    }

    @Test
    @DisplayName("Objetivos afines puntuan la mitad aunque no sean iguales")
    void objetivosAfines() {
        Gimnasio g = gimnasio(1L, "Gym");
        List<Disponibilidad> h = List.of(franja("Lunes", "18:00", "19:00"));

        PuntuacionCompatibilidad p = CalculadoraCompatibilidad.calcular(
                usuario("Intermedio", "Hipertrofia", 30, g), h,
                usuario("Intermedio", "Fuerza", 30, g), h);

        assertEquals(10, puntosDe(p, "objetivo"));
    }

    @Test
    @DisplayName("Los factores siempre suman exactamente el total")
    void elDesgloseCuadraConElTotal() {
        Gimnasio g = gimnasio(1L, "Gym");
        List<Disponibilidad> h = List.of(franja("Martes", "19:00", "21:00"));

        PuntuacionCompatibilidad p = CalculadoraCompatibilidad.calcular(
                usuario("Avanzado", "Resistencia", 41, g), h,
                usuario("Intermedio", "Perdida de peso", 25, g), h);

        double suma = p.factores().stream().mapToDouble(FactorCompatibilidad::puntos).sum();
        assertEquals(p.total(), Math.round(suma));
    }

    @Test
    @DisplayName("Un perfil incompleto puntua bajo pero no lanza excepciones")
    void perfilIncompletoNoRompe() {
        Usuario vacio = usuario(null, null, null, null);
        PuntuacionCompatibilidad p = CalculadoraCompatibilidad.calcular(
                vacio, List.of(), vacio, List.of());

        assertEquals(0, p.total());
        assertEquals("Poca compatibilidad", p.etiqueta());
        assertEquals(5, p.factores().size());
    }

    @Test
    @DisplayName("El factor dominante ignora los que no se han podido evaluar")
    void factorDominante() {
        Gimnasio g = gimnasio(1L, "Gym");
        // Compromiso mutuo tres dias: el horario debe ser lo que mande, pese a que
        // nivel y objetivo esten en las antipodas.
        List<Disponibilidad> h = List.of(
                fija("Lunes", "18:00", "21:00"),
                fija("Miercoles", "18:00", "21:00"),
                fija("Viernes", "18:00", "21:00"));

        PuntuacionCompatibilidad p = CalculadoraCompatibilidad.calcular(
                usuario("Principiante", "Fuerza", 20, g), h,
                usuario("Avanzado", "Resistencia", 45, g), h);

        assertEquals("horario", p.factorDominante().nombre());
        assertTrue(p.solape().hayAncla());
    }

    @Test
    @DisplayName("Sin compromiso, el horario deja de mandar frente a los demas factores")
    void sinCompromisoElHorarioPesaMenos() {
        Gimnasio g = gimnasio(1L, "Gym");
        List<Disponibilidad> h = List.of(
                franja("Lunes", "18:00", "21:00"),
                franja("Miercoles", "18:00", "21:00"));

        PuntuacionCompatibilidad p = CalculadoraCompatibilidad.calcular(
                usuario("Intermedio", "Hipertrofia", 30, g), h,
                usuario("Intermedio", "Hipertrofia", 30, g), h);

        // Con disponibilidad vaga, nivel y objetivo aportan mas que el horario:
        // es la respuesta correcta a "puedo a muchas horas" y evita que declarar
        // rango de sobra sea la mejor estrategia.
        double horario = puntosDe(p, "horario");
        assertTrue(horario < puntosDe(p, "nivel"),
                "El horario sin compromiso saco %.1f".formatted(horario));
    }
}
