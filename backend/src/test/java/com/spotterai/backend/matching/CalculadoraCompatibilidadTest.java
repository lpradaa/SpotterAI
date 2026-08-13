package com.spotterai.backend.matching;

import com.spotterai.backend.textos.TextosDePrueba;
import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.models.Levantamiento;
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

    private static Levantamiento levantamiento(Ejercicio ejercicio, double peso, int reps) {
        Levantamiento l = new Levantamiento();
        l.setEjercicio(ejercicio);
        l.setPeso(peso);
        l.setRepeticiones(reps);
        return l;
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

/**
     * El mismo usuario, ya con rutina.
     *
     * Aparte y no dentro de {@code usuario(...)} porque hay pruebas que necesitan
     * justo lo contrario: un perfil sin nada, ni siquiera rutina.
     */
    private static Usuario conRutina(Usuario u, Rutina rutina) {
        u.setRutina(rutina.name());
        return u;
    }

/**
     * Alguien que aparece: doce sesiones en cuatro semanas.
     *
     * Desde que la constancia es un factor, "perfil completo" incluye haber
     * entrenado. Sin esto el techo se queda por debajo de 100 por el descuento
     * de evidencia, que es exactamente lo que se quiere.
     */
    private static Constancia constante() {
        return new Constancia(12, true);
    }

    /** Dos perfiles completos, con todo lo que el motor sabe mirar. */
    private static PuntuacionCompatibilidad puntuar(
            Usuario a, List<Disponibilidad> horariosA, List<Levantamiento> pesosA,
            Usuario b, List<Disponibilidad> horariosB, List<Levantamiento> pesosB) {
        return CalculadoraCompatibilidad.calcular(
                new PerfilDeMatch(a, horariosA, pesosA, constante()),
                new PerfilDeMatch(b, horariosB, pesosB, constante()));
    }

/**
     * Puntua un par de perfiles sin marcas ni constancia.
     *
     * <p>La calculadora ya no acepta llamadas a medias: quien no tiene un dato
     * lo dice construyendo el {@link PerfilDeMatch} incompleto a proposito.
     * Estas pruebas van justamente de los factores que si pasan, asi que la
     * ausencia esta declarada aqui, una vez, en vez de escondida en que
     * sobrecarga elegia el compilador.
     */
    private static PuntuacionCompatibilidad calcularSoloConHorarios(
            Usuario a, List<Disponibilidad> horariosA,
            Usuario b, List<Disponibilidad> horariosB) {
        return CalculadoraCompatibilidad.calcular(
                PerfilDeMatch.de(a, horariosA), PerfilDeMatch.de(b, horariosB));
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
        Usuario a = conRutina(usuario("Intermedio", "Hipertrofia", 28, g), Rutina.TORSO_PIERNA);
        Usuario b = conRutina(usuario("Intermedio", "Hipertrofia", 29, g), Rutina.TORSO_PIERNA);

        // Las tres franjas con compromiso mutuo: el maximo del tope permitido
        List<Disponibilidad> conCompromiso = List.of(
                fija("Lunes", "18:00", "20:00"),
                fija("Miercoles", "18:00", "20:00"),
                fija("Viernes", "18:00", "20:00"));

        // Con levantamientos tambien: desde que la fuerza es un factor, un perfil
        // sin ellos tiene el techo en 93 por el descuento de evidencia. Que el 100
        // exija el perfil entero es justo lo que se quiere.
        List<Levantamiento> pesos = List.of(levantamiento(Ejercicio.PRESS_BANCA, 100, 5));

        assertEquals(100, puntuar(a, conCompromiso, pesos, b, conCompromiso, pesos).total());

        assertTrue(calcularSoloConHorarios(a, conCompromiso, b, conCompromiso).total() < 100,
                "Sin levantamientos no se puede llegar al maximo");

        // Basta con que una de las tres sea solo disponibilidad para no llegar al tope
        List<Disponibilidad> casiTodoFirme = List.of(
                fija("Lunes", "18:00", "20:00"),
                fija("Miercoles", "18:00", "20:00"),
                franja("Viernes", "18:00", "20:00"));

        assertTrue(calcularSoloConHorarios(a, casiTodoFirme, b, casiTodoFirme).total() < 100);

        // Las mismas horas, pero sin que ninguno se comprometa, no llegan al maximo:
        // es disponibilidad, no una cita.
        List<Disponibilidad> soloDisponibles = List.of(
                franja("Lunes", "18:00", "20:00"),
                franja("Miercoles", "18:00", "20:00"),
                franja("Viernes", "18:00", "20:00"));

        PuntuacionCompatibilidad vaga =
                calcularSoloConHorarios(a, soloDisponibles, b, soloDisponibles);
        assertTrue(vaga.total() < 100);
        assertTrue(vaga.total() > 60, "Aun asi debe puntuar bien: %d".formatted(vaga.total()));
    }

    @Test
    @DisplayName("Sin solape horario se pierden los 40 puntos aunque el resto encaje")
    void sinSolapeHorarioPierde40() {
        Gimnasio g = gimnasio(1L, "McFit Centro");
        Usuario a = usuario("Intermedio", "Hipertrofia", 28, g);
        Usuario b = usuario("Intermedio", "Hipertrofia", 28, g);

        PuntuacionCompatibilidad p = calcularSoloConHorarios(
                a, List.of(franja("Lunes", "07:00", "09:00")),
                b, List.of(franja("Lunes", "20:00", "22:00")));

        assertEquals(0, puntosDe(p, "horario"));

        // Se compara contra el mismo par pero con los horarios cruzados, en vez de
        // contra una constante: el total exacto depende del reparto de los factores
        // sin datos, asi que un numero fijo aqui se rompe cada vez que se toca un
        // peso sin que el comportamiento vigilado haya cambiado.
        // Con compromiso mutuo, para que el solape valga lo que puede valer: dos
        // franjas "puedo ir" apenas se separan de no coincidir, y esa comparacion
        // no distinguiria nada.
        PuntuacionCompatibilidad conSolape = calcularSoloConHorarios(
                a, List.of(fija("Lunes", "20:00", "22:00")),
                b, List.of(fija("Lunes", "20:00", "22:00")));

        assertTrue(p.total() < conSolape.total() - 25,
                "Perder el horario entero deberia costar mucho: %d frente a %d"
                        .formatted(p.total(), conSolape.total()));
    }

    @Test
    @DisplayName("Niveles contiguos puntuan la mitad; extremos puntuan cero")
    void escalaDeNivel() {
        Gimnasio g = gimnasio(1L, "Gym");
        List<Disponibilidad> h = List.of(franja("Lunes", "18:00", "19:00"));

        PuntuacionCompatibilidad iguales = calcularSoloConHorarios(
                usuario("Intermedio", "Fuerza", 30, g), h,
                usuario("Intermedio", "Fuerza", 30, g), h);

        PuntuacionCompatibilidad contiguos = calcularSoloConHorarios(
                usuario("Principiante", "Fuerza", 30, g), h,
                usuario("Intermedio", "Fuerza", 30, g), h);

        PuntuacionCompatibilidad extremos = calcularSoloConHorarios(
                usuario("Principiante", "Fuerza", 30, g), h,
                usuario("Avanzado", "Fuerza", 30, g), h);

        // La regla es "la mitad", no "diez puntos": los puntos absolutos dependen
        // del peso del factor y del reparto, y ambos han cambiado ya una vez.
        assertEquals(puntosDe(iguales, "nivel") / 2.0, puntosDe(contiguos, "nivel"), 0.01);
        assertEquals(0, puntosDe(extremos, "nivel"));
    }

    @Test
    @DisplayName("Objetivos afines puntuan la mitad aunque no sean iguales")
    void objetivosAfines() {
        Gimnasio g = gimnasio(1L, "Gym");
        List<Disponibilidad> h = List.of(franja("Lunes", "18:00", "19:00"));

        PuntuacionCompatibilidad afines = calcularSoloConHorarios(
                usuario("Intermedio", "Hipertrofia", 30, g), h,
                usuario("Intermedio", "Fuerza", 30, g), h);

        PuntuacionCompatibilidad iguales = calcularSoloConHorarios(
                usuario("Intermedio", "Fuerza", 30, g), h,
                usuario("Intermedio", "Fuerza", 30, g), h);

        // Afines valen la mitad que iguales, sea cual sea el peso del factor
        assertEquals(puntosDe(iguales, "objetivo") / 2.0, puntosDe(afines, "objetivo"), 0.01);
    }

    @Test
    @DisplayName("Los factores siempre suman exactamente el total")
    void elDesgloseCuadraConElTotal() {
        Gimnasio g = gimnasio(1L, "Gym");
        List<Disponibilidad> h = List.of(franja("Martes", "19:00", "21:00"));

        // Con el perfil completo, para que no entre el descuento por evidencia:
        // cuando falta algun dato, el total es la suma multiplicada por la
        // confianza y deja de cuadrar con el desglose a proposito.
        List<Levantamiento> pesos = List.of(levantamiento(Ejercicio.SENTADILLA, 100, 5));

        PuntuacionCompatibilidad p = puntuar(
                conRutina(usuario("Avanzado", "Resistencia", 41, g), Rutina.WEIDER), h, pesos,
                conRutina(usuario("Intermedio", "Perdida de peso", 25, g), Rutina.FULL_BODY), h, pesos);

        double suma = p.factores().stream().mapToDouble(FactorCompatibilidad::puntos).sum();
        assertEquals(p.total(), Math.round(suma));
    }

    @Test
    @DisplayName("Un perfil incompleto puntua bajo pero no lanza excepciones")
    void perfilIncompletoNoRompe() {
        Usuario vacio = usuario(null, null, null, null);
        PuntuacionCompatibilidad p = calcularSoloConHorarios(
                vacio, List.of(), vacio, List.of());

        assertEquals(0, p.total());
        assertEquals("Poca compatibilidad", TextosDePrueba.nuevo().de(p.etiqueta()));
        // Un factor por cada dimension que evalua el motor. Se comprueba que
        // ninguna se pierda por el camino, no un numero concreto de memoria.
        assertEquals(8, p.factores().size());
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

        PuntuacionCompatibilidad p = calcularSoloConHorarios(
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

        PuntuacionCompatibilidad p = calcularSoloConHorarios(
                usuario("Intermedio", "Hipertrofia", 30, g), h,
                usuario("Intermedio", "Hipertrofia", 30, g), h);

        // Declarar rango de sobra sin comprometerse no puede acercarse al maximo
        // del factor: es la respuesta correcta a "puedo a muchas horas".
        //
        // Antes se comparaba contra los puntos del nivel, pero eso ataba el test a
        // un peso concreto y se rompio al bajar el nivel de 20 a 10. Lo que
        // importa es la fraccion del propio factor.
        FactorCompatibilidad horario = p.factores().stream()
                .filter(f -> f.nombre().equals("horario")).findFirst().orElseThrow();

        double fraccion = horario.puntos() / horario.puntosMax();
        assertTrue(fraccion < 0.5,
                "El horario sin compromiso saco el %.0f %% de su maximo".formatted(fraccion * 100));
    }
}
