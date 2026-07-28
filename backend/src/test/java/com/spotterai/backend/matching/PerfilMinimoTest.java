package com.spotterai.backend.matching;

import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.models.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lo que hay que declarar para poder emparejarse.
 *
 * <p>Lo que se protege aquí es sobre todo la frontera: qué se exige y qué no.
 * Es fácil que alguien añada un campo al perfil y lo meta aquí «por
 * consistencia», y cada campo obligatorio de más es una pantalla que la gente
 * rellena a lo que sea con tal de pasar.
 */
class PerfilMinimoTest {

    private static Usuario completo() {
        Gimnasio g = new Gimnasio();
        g.setId(1L);
        g.setNombre("McFit");

        Usuario u = new Usuario();
        u.setNivel("Intermedio");
        u.setObjetivos("Hipertrofia");
        u.setEdad(28);
        u.setRutina("TORSO_PIERNA");
        u.setGimnasio(g);
        return u;
    }

    @Test
    @DisplayName("Con las cinco declaraciones y el horario, ya se puede emparejar")
    void completoEsCompleto() {
        assertTrue(PerfilMinimo.de(completo(), true).estaCompleto());
    }

    @Test
    @DisplayName("Un perfil vacío no tiene nada de lo que hace falta")
    void vacioLoPideTodo() {
        PerfilMinimo m = PerfilMinimo.de(new Usuario(), false);

        assertFalse(m.estaCompleto());
        assertEquals(
                java.util.List.of("horarios", "gimnasioId", "nivel", "objetivos", "rutina", "edad"),
                m.queFalta());
    }

    @Test
    @DisplayName("El horario se pide primero: es lo único sin lo que no hay match posible")
    void elHorarioVaPrimero() {
        assertEquals("horarios", PerfilMinimo.de(new Usuario(), false).queFalta().get(0));
    }

    @Test
    @DisplayName("Los levantamientos NO se exigen: obligarlos produce números inventados")
    void lasMarcasNoSeExigen() {
        // Es la decisión que separa esto de "hacerlo todo required". Una medición
        // no se puede pedir a quien no la sabe sin recibir un número a boleo, y
        // ese número entraría derecho al factor del que depende el nombre del
        // producto. Un hueco al menos sabe que lo es.
        assertTrue(PerfilMinimo.de(completo(), true).estaCompleto());
        assertFalse(PerfilMinimo.de(completo(), true).queFalta().contains("levantamientos"));
    }

    @Test
    @DisplayName("Un campo en blanco cuenta como que falta, no solo el nulo")
    void enBlancoEsQueFalta() {
        Usuario u = completo();
        u.setNivel("   ");

        assertTrue(PerfilMinimo.de(u, true).queFalta().contains("nivel"));
    }

    @Test
    @DisplayName("Una rutina que no existe cuenta como que falta")
    void rutinaIlegibleEsQueFalta() {
        Usuario u = completo();
        u.setRutina("ARNOLD_SPLIT");

        assertTrue(PerfilMinimo.de(u, true).queFalta().contains("rutina"));
    }

    @Test
    @DisplayName("«Sin rutina fija» sí vale: es una respuesta, no un hueco")
    void libreEsUnaRespuesta() {
        Usuario u = completo();
        u.setRutina(Rutina.LIBRE.name());

        // Sin esta salida, exigir el campo solo consigue que quien no sigue
        // ninguna rutina marque la primera de la lista, y entonces el motor está
        // más seguro de un dato peor.
        assertTrue(PerfilMinimo.de(u, true).estaCompleto());
    }
}
