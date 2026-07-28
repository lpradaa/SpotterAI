package com.spotterai.backend.matching;

import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.models.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lo que se protege aqui es que el aviso diga la verdad.
 *
 * Si los puntos que se anuncian se separan de los que usa la calculadora, la
 * aplicacion pasa a mentir sobre lo que cuesta no rellenar algo, y eso es peor
 * que no avisar.
 */
class RendimientoDelPerfilTest {

    /** Alguien que aparece: doce sesiones en cuatro semanas. */
    private static final Constancia CONSTANTE = new Constancia(12, true);

    private static Usuario perfilCompleto() {
        Usuario u = new Usuario();
        u.setNivel("Intermedio");
        u.setObjetivos("Hipertrofia");
        u.setEdad(28);
        Gimnasio g = new Gimnasio();
        g.setId(1L);
        g.setNombre("McFit");
        u.setGimnasio(g);
        u.setRutina("TORSO_PIERNA");
        return u;
    }

    @Test
    @DisplayName("Un perfil completo no tiene nada en juego")
    void perfilCompletoNoAvisa() {
        RendimientoDelPerfil r = RendimientoDelPerfil.de(perfilCompleto(), true, true, CONSTANTE);

        assertTrue(r.estaCompleto());
        assertEquals(0, r.puntosEnJuego());
        assertNull(r.masCaro());
    }

    @Test
    @DisplayName("Sin horario se pierden los 40 puntos del horario")
    void faltaElHorario() {
        RendimientoDelPerfil r = RendimientoDelPerfil.de(perfilCompleto(), false, true, CONSTANTE);

        assertEquals((int) CalculadoraCompatibilidad.PESO_HORARIO, r.puntosEnJuego());
        assertEquals("horarios", r.masCaro().campo());
    }

    @Test
    @DisplayName("Lo que mas cuesta va primero, no en el orden en que se comprueba")
    void ordenPorCoste() {
        Usuario vacio = new Usuario();

        RendimientoDelPerfil r = RendimientoDelPerfil.de(vacio, false, false, Constancia.DESCONOCIDA);

        assertEquals("horarios", r.huecos().get(0).campo());
        assertEquals((int) CalculadoraCompatibilidad.PESO_HORARIO, r.huecos().get(0).puntos());
        // Edad pesa 5: la ultima, aunque se comprueba antes que otras cosas.
        assertEquals("edad", r.huecos().get(r.huecos().size() - 1).campo());
    }

    @Test
    @DisplayName("Un perfil vacio pone en juego los 100 puntos")
    void perfilVacio() {
        RendimientoDelPerfil r = RendimientoDelPerfil.de(new Usuario(), false, false, Constancia.DESCONOCIDA);

        // Si esto deja de sumar 100 es que los pesos de la calculadora han
        // cambiado sin que el aviso se entere, o que falta un factor.
        assertEquals(100, r.puntosEnJuego());
        assertEquals(8, r.huecos().size());
    }

    @Test
    @DisplayName("Los puntos anunciados son los mismos que usa la calculadora")
    void lospuntosSalenDeLaCalculadora() {
        RendimientoDelPerfil r = RendimientoDelPerfil.de(new Usuario(), false, false, Constancia.DESCONOCIDA);

        assertEquals((int) CalculadoraCompatibilidad.PESO_HORARIO, puntosDe(r, "horarios"));
        assertEquals((int) CalculadoraCompatibilidad.PESO_NIVEL, puntosDe(r, "nivel"));
        assertEquals((int) CalculadoraCompatibilidad.PESO_FUERZA, puntosDe(r, "levantamientos"));
        assertEquals((int) CalculadoraCompatibilidad.PESO_OBJETIVO, puntosDe(r, "objetivos"));
        assertEquals((int) CalculadoraCompatibilidad.PESO_GIMNASIO, puntosDe(r, "gimnasioId"));
        assertEquals((int) CalculadoraCompatibilidad.PESO_RUTINA, puntosDe(r, "rutina"));
        assertEquals((int) CalculadoraCompatibilidad.PESO_CONSTANCIA, puntosDe(r, "entrenamientos"));
        assertEquals((int) CalculadoraCompatibilidad.PESO_EDAD, puntosDe(r, "edad"));
    }

    @Test
    @DisplayName("Un campo en blanco cuenta como que falta, no solo el nulo")
    void cadenaVaciaCuentaComoQueFalta() {
        Usuario u = perfilCompleto();
        u.setObjetivos("   ");

        RendimientoDelPerfil r = RendimientoDelPerfil.de(u, true, true, CONSTANTE);

        assertEquals((int) CalculadoraCompatibilidad.PESO_OBJETIVO, r.puntosEnJuego());
        assertEquals("objetivos", r.masCaro().campo());
    }

    private static int puntosDe(RendimientoDelPerfil r, String campo) {
        return r.huecos().stream()
                .filter(h -> h.campo().equals(campo))
                .findFirst().orElseThrow().puntos();
    }

    @Test
    @DisplayName("Entrenar poco se avisa: si no, la nota baja con todos y nadie sabe por que")
    void laConstanciaFlojaSeAvisa() {
        // Dos sesiones en cuatro semanas. La constancia de una pareja es la del
        // que menos aparece, asi que esto resta con todo el mundo a la vez.
        RendimientoDelPerfil r = RendimientoDelPerfil.de(
                perfilCompleto(), true, true, new Constancia(2, true));

        HuecoDelPerfil hueco = r.huecos().stream()
                .filter(h -> h.campo().equals("constancia"))
                .findFirst().orElseThrow(() -> new AssertionError("No avisa de la constancia floja"));

        assertTrue(hueco.puntos() > 0);
        assertTrue(hueco.motivo().contains("2 entrenamientos"), hueco.motivo());
    }

    @Test
    @DisplayName("A quien ya entrena con regularidad no se le da la lata")
    void alConstanteNoSeLeAvisa() {
        RendimientoDelPerfil r = RendimientoDelPerfil.de(perfilCompleto(), true, true, CONSTANTE);

        assertTrue(r.estaCompleto(), "Un perfil completo y constante no tiene nada que decir");
    }

    @Test
    @DisplayName("Sin historial el aviso es otro: no es que entrenes poco, es que no se sabe")
    void sinHistorialElAvisoEsOtro() {
        RendimientoDelPerfil r = RendimientoDelPerfil.de(
                perfilCompleto(), true, true, Constancia.DESCONOCIDA);

        // Quien acaba de entrar no ha hecho nada mal; le falta un dato.
        assertTrue(r.huecos().stream().anyMatch(h -> h.campo().equals("entrenamientos")));
        assertTrue(r.huecos().stream().noneMatch(h -> h.campo().equals("constancia")));
    }
}
