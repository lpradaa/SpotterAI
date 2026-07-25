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
 * Un factor sin datos debe salir del calculo y repartir su peso, no restar.
 *
 * <p>Antes, no rellenar un campo puntuaba igual que rellenarlo mal: quien no tenia
 * horarios sacaba 0 de 40 contra todo el mundo y parecia incompatible con todos.
 */
class RepartoDePesoTest {

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

    private static Disponibilidad franja(String dia, String inicio, String fin) {
        return new Disponibilidad(dia, LocalTime.parse(inicio), LocalTime.parse(fin), null, true);
    }

    @Test
    @DisplayName("Sin horarios se reparte el peso, pero la nota no llega al maximo")
    void faltaDeHorariosNoPenaliza() {
        Gimnasio g = gimnasio(1L);
        Usuario a = usuario("Intermedio", "Hipertrofia", 30, g);
        Usuario b = usuario("Intermedio", "Hipertrofia", 30, g);

        PuntuacionCompatibilidad p = CalculadoraCompatibilidad.calcular(a, List.of(), b, List.of());

        // Todo lo conocido encaja, asi que no se hunde como antes (sacaba 60 de tope
        // y la etiqueta decia "poca compatibilidad")...
        assertTrue(p.total() > 60);
        // ...pero tampoco puede alcanzar el 100 de un perfil completo: falta el
        // factor que mas pesa y eso tiene que notarse.
        assertTrue(p.total() < 100, "Sin horarios no deberia dar 100, dio %d".formatted(p.total()));
        assertFalse(p.esCompleta());
        assertEquals(List.of("horario"), p.factoresSinDatos());
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

        int puntosSinDatos = CalculadoraCompatibilidad
                .calcular(yo, misFranjas, sinHorarios, List.of()).total();
        int puntosConSolape = CalculadoraCompatibilidad
                .calcular(yo, misFranjas, conSolape, misFranjas).total();

        assertTrue(puntosConSolape > puntosSinDatos,
                "Con solape real saco %d y sin ningun dato %d"
                        .formatted(puntosConSolape, puntosSinDatos));
    }

    @Test
    @DisplayName("El factor omitido no aporta puntos ni peso")
    void elFactorOmitidoQuedaMarcado() {
        Gimnasio g = gimnasio(1L);
        PuntuacionCompatibilidad p = CalculadoraCompatibilidad.calcular(
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

        PuntuacionCompatibilidad p = CalculadoraCompatibilidad.calcular(
                usuario("Intermedio", "Hipertrofia", 30, g), h,
                usuario("Intermedio", "Hipertrofia", 30, g), h);

        assertTrue(p.esCompleta());
        assertTrue(p.factoresSinDatos().isEmpty());
    }

    @Test
    @DisplayName("Los horarios de un solo lado tampoco bastan para evaluar el solape")
    void hacenFaltaLosDosLados() {
        Gimnasio g = gimnasio(1L);
        PuntuacionCompatibilidad p = CalculadoraCompatibilidad.calcular(
                usuario("Intermedio", "Hipertrofia", 30, g),
                List.of(franja("Lunes", "18:00", "20:00")),
                usuario("Intermedio", "Hipertrofia", 30, g),
                List.of());

        assertEquals(List.of("horario"), p.factoresSinDatos());
    }

    @Test
    @DisplayName("Cuantos menos factores conocidos, menor es el techo de la nota")
    void elTechoSubeConLaEvidencia() {
        Gimnasio g = gimnasio(1L);
        List<Disponibilidad> h = List.of(franja("Lunes", "18:00", "20:00"));

        // Solo se conoce el nivel: coincide, pero es muy poco en lo que basarse
        int soloNivel = CalculadoraCompatibilidad.calcular(
                usuario("Intermedio", null, null, null), List.of(),
                usuario("Intermedio", null, null, null), List.of()).total();

        // Se conoce todo menos el horario
        int casiTodo = CalculadoraCompatibilidad.calcular(
                usuario("Intermedio", "Hipertrofia", 30, g), List.of(),
                usuario("Intermedio", "Hipertrofia", 30, g), List.of()).total();

        // Perfil completo
        int completo = CalculadoraCompatibilidad.calcular(
                usuario("Intermedio", "Hipertrofia", 30, g), h,
                usuario("Intermedio", "Hipertrofia", 30, g), h).total();

        assertTrue(soloNivel < casiTodo, "%d deberia ser menor que %d".formatted(soloNivel, casiTodo));
        assertTrue(casiTodo < completo, "%d deberia ser menor que %d".formatted(casiTodo, completo));
    }

    @Test
    @DisplayName("Sin ningun dato comun la puntuacion es cero y se dice que esta incompleta")
    void sinNingunDato() {
        Usuario vacio = usuario(null, null, null, null);
        PuntuacionCompatibilidad p = CalculadoraCompatibilidad.calcular(
                vacio, List.of(), vacio, List.of());

        assertEquals(0, p.total());
        assertFalse(p.esCompleta());
        assertEquals(5, p.factoresSinDatos().size());
    }

    @Test
    @DisplayName("Un mal encaje sigue restando: no confundir 'sin datos' con 'no encajais'")
    void elMalEncajeSiguePenalizando() {
        Gimnasio uno = gimnasio(1L);
        Gimnasio otro = gimnasio(2L);

        PuntuacionCompatibilidad p = CalculadoraCompatibilidad.calcular(
                usuario("Principiante", "Fuerza", 20, uno), List.of(),
                usuario("Avanzado", "Resistencia", 55, otro), List.of());

        assertTrue(p.total() < 30,
                "Datos completos pero incompatibles deben puntuar bajo, saco %d".formatted(p.total()));
        assertEquals(List.of("horario"), p.factoresSinDatos());
    }
}
