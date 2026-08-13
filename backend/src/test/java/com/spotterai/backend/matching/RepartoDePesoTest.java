package com.spotterai.backend.matching;

import com.spotterai.backend.semantica.VectorDePrueba;
import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.models.Levantamiento;
import com.spotterai.backend.models.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Un factor sin datos debe salir del calculo y repartir su peso, no restar.
 *
 * <p>Antes, no rellenar un campo puntuaba igual que rellenarlo mal: quien no tenia
 * horarios sacaba 0 de 40 contra todo el mundo y parecia incompatible con todos.
 */
class RepartoDePesoTest {

    private static Levantamiento levantamiento(Ejercicio ejercicio, double peso, int reps) {
        Levantamiento l = new Levantamiento();
        l.setEjercicio(ejercicio);
        l.setPeso(peso);
        l.setRepeticiones(reps);
        return l;
    }

    private static Gimnasio gimnasio(long id) {
        Gimnasio g = new Gimnasio();
        g.setId(id);
        g.setNombre("McFit");
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

    private static Disponibilidad franja(String dia, String inicio, String fin) {
        return new Disponibilidad(dia, LocalTime.parse(inicio), LocalTime.parse(fin), null, true);
    }

    @Test
    @DisplayName("Sin horarios se reparte el peso, pero la nota no llega al maximo")
    void faltaDeHorariosNoPenaliza() {
        Gimnasio g = gimnasio(1L);
        Usuario a = usuario("Intermedio", "Hipertrofia", 30, g);
        Usuario b = usuario("Intermedio", "Hipertrofia", 30, g);

        PuntuacionCompatibilidad p = calcularSoloConHorarios(a, List.of(), b, List.of());

        // Todo lo conocido encaja, asi que no se hunde como antes, cuando la falta
        // de horarios restaba y la etiqueta decia "poca compatibilidad". Se compara
        // contra la misma situacion con los datos conocidos en desacuerdo, y no
        // contra un numero: el umbral absoluto se queda viejo cada vez que cambia
        // el reparto de pesos, y entonces la prueba se pone roja sin que nada se
        // haya roto.
        Gimnasio otroGimnasio = gimnasio(2L);
        PuntuacionCompatibilidad enDesacuerdo = calcularSoloConHorarios(
                usuario("Principiante", "Resistencia", 20, g), List.of(),
                usuario("Avanzado", "Hipertrofia", 45, otroGimnasio), List.of());

        assertTrue(p.total() > enDesacuerdo.total() * 2,
                "Coincidir en todo lo conocido tiene que valer mucho mas que no coincidir: %d frente a %d"
                        .formatted(p.total(), enDesacuerdo.total()));
        // ...pero tampoco puede alcanzar el 100 de un perfil completo: falta el
        // factor que mas pesa y eso tiene que notarse.
        assertTrue(p.total() < 100, "Sin horarios no deberia dar 100, dio %d".formatted(p.total()));
        assertFalse(p.esCompleta());
        assertTrue(p.factoresSinDatos().contains("horario"),
                "Sin horarios, el factor horario tiene que quedar fuera del calculo");
    }

    @Test
    @DisplayName("Un perfil vacio nunca adelanta a uno completo con solape real")
    void masDatosNuncaPuntuanPeorQueMenos() {
        // Este es el fallo que aparecio al medir contra los datos reales: dos usuarios
        // sin ningun horario encabezaban la lista con 100, por delante de otros con
        // seis horas semanales de coincidencia. Tener menos informacion daba mejor
        // nota, que es exactamente al reves de lo que debe pasar.
        Gimnasio g = gimnasio(1L);
        Usuario yo = usuario("Intermedio", "Hipertrofia", 30, g);

        Usuario sinHorarios = usuario("Intermedio", "Hipertrofia", 30, g);
        Usuario conSolape = usuario("Intermedio", "Hipertrofia", 30, g);

        List<Disponibilidad> misFranjas = List.of(franja("Lunes", "18:00", "20:00"));

        int puntosSinDatos = calcularSoloConHorarios(yo, misFranjas, sinHorarios, List.of()).total();
        int puntosConSolape = calcularSoloConHorarios(yo, misFranjas, conSolape, misFranjas).total();

        assertTrue(puntosConSolape > puntosSinDatos,
                "Con solape real saco %d y sin ningun dato %d"
                        .formatted(puntosConSolape, puntosSinDatos));
    }

    @Test
    @DisplayName("El factor omitido no aporta puntos ni peso")
    void elFactorOmitidoQuedaMarcado() {
        Gimnasio g = gimnasio(1L);
        PuntuacionCompatibilidad p = calcularSoloConHorarios(
                usuario("Intermedio", "Hipertrofia", 30, g), List.of(),
                usuario("Intermedio", "Hipertrofia", 30, g), List.of());

        FactorCompatibilidad horario = p.factores().stream()
                .filter(f -> f.nombre().equals("horario")).findFirst().orElseThrow();

        assertFalse(horario.aplicable());
        assertEquals(0, horario.puntos());
        assertEquals(0, horario.puntosMax());
    }

    @Test
    @DisplayName("Con horarios en ambos lados el factor vuelve a contar")
    void conDatosSeEvaluaNormalmente() {
        Gimnasio g = gimnasio(1L);
        List<Disponibilidad> h = List.of(franja("Lunes", "18:00", "20:00"));

        // "Completo" incluye ahora los levantamientos y la rutina: sin ellos el
        // factor de fuerza o el de rutina se quedan sin datos y el perfil no llega
        // a estarlo.
        List<Levantamiento> pesos = List.of(levantamiento(Ejercicio.PRESS_BANCA, 90, 5));

        // Con biografia: desde que la afinidad de lo escrito es un factor,
        // "perfil completo" incluye haber escrito algo sobre uno mismo.
        Usuario unoCompleto = VectorDePrueba.con(
                conRutina(usuario("Intermedio", "Hipertrofia", 30, g), Rutina.TORSO_PIERNA), 1.0);
        Usuario otroCompleto = VectorDePrueba.con(
                conRutina(usuario("Intermedio", "Hipertrofia", 30, g), Rutina.TORSO_PIERNA), 1.0);

        PuntuacionCompatibilidad p = puntuar(unoCompleto, h, pesos, otroCompleto, h, pesos);

        assertTrue(p.esCompleta());
        assertTrue(p.factoresSinDatos().isEmpty());
    }

    @Test
    @DisplayName("Los horarios de un solo lado tampoco bastan para evaluar el solape")
    void hacenFaltaLosDosLados() {
        Gimnasio g = gimnasio(1L);
        PuntuacionCompatibilidad p = calcularSoloConHorarios(
                usuario("Intermedio", "Hipertrofia", 30, g),
                List.of(franja("Lunes", "18:00", "20:00")),
                usuario("Intermedio", "Hipertrofia", 30, g),
                List.of());

        assertTrue(p.factoresSinDatos().contains("horario"),
                "Sin horarios, el factor horario tiene que quedar fuera del calculo");
    }

    @Test
    @DisplayName("Cuantos menos factores conocidos, menor es el techo de la nota")
    void elTechoSubeConLaEvidencia() {
        Gimnasio g = gimnasio(1L);
        List<Disponibilidad> h = List.of(franja("Lunes", "18:00", "20:00"));

        // Solo se conoce el nivel: coincide, pero es muy poco en lo que basarse
        int soloNivel = calcularSoloConHorarios(
                usuario("Intermedio", null, null, null), List.of(),
                usuario("Intermedio", null, null, null), List.of()).total();

        // Se conoce todo menos el horario
        int casiTodo = calcularSoloConHorarios(
                usuario("Intermedio", "Hipertrofia", 30, g), List.of(),
                usuario("Intermedio", "Hipertrofia", 30, g), List.of()).total();

        // Perfil completo
        int completo = calcularSoloConHorarios(
                usuario("Intermedio", "Hipertrofia", 30, g), h,
                usuario("Intermedio", "Hipertrofia", 30, g), h).total();

        assertTrue(soloNivel < casiTodo, "%d deberia ser menor que %d".formatted(soloNivel, casiTodo));
        assertTrue(casiTodo < completo, "%d deberia ser menor que %d".formatted(casiTodo, completo));
    }

    @Test
    @DisplayName("Sin ningun dato comun la puntuacion es cero y se dice que esta incompleta")
    void sinNingunDato() {
        Usuario vacio = usuario(null, null, null, null);
        PuntuacionCompatibilidad p = calcularSoloConHorarios(
                vacio, List.of(), vacio, List.of());

        assertEquals(0, p.total());
        assertFalse(p.esCompleta());
        assertEquals(9, p.factoresSinDatos().size());
    }

    @Test
    @DisplayName("Un mal encaje sigue restando: no confundir 'sin datos' con 'no encajais'")
    void elMalEncajeSiguePenalizando() {
        Gimnasio uno = gimnasio(1L);
        Gimnasio otro = gimnasio(2L);

        PuntuacionCompatibilidad p = calcularSoloConHorarios(
                usuario("Principiante", "Fuerza", 20, uno), List.of(),
                usuario("Avanzado", "Resistencia", 55, otro), List.of());

        assertTrue(p.total() < 30,
                "Datos completos pero incompatibles deben puntuar bajo, saco %d".formatted(p.total()));
        assertTrue(p.factoresSinDatos().contains("horario"),
                "Sin horarios, el factor horario tiene que quedar fuera del calculo");
    }
}
