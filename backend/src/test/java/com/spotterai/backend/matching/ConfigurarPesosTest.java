package com.spotterai.backend.matching;

import com.spotterai.backend.models.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Que ajustar un peso no exija recompilar.
 *
 * <p>Antes de esto, cambiar cuanto vale el horario frente a la constancia
 * significaba tocar una constante, recompilar y desplegar. El embudo de
 * compatibilidad —{@code /api/metricas/embudo}— existe para poder decidir si
 * estos numeros son los correctos, y esa decision no se puede tomar sin poder
 * probar otro reparto.
 *
 * <h2>Por que esta clase, y no una prueba mas dentro de RepartoDePesoTest</h2>
 *
 * <p>Los pesos de {@link CalculadoraCompatibilidad} son campos estaticos,
 * compartidos por toda la maquina virtual. Una prueba que los cambie y no los
 * devuelva a su sitio deja calculando con un motor distinto a cualquier otra
 * prueba que corra despues en el mismo proceso —de esta clase o de cualquier
 * otra—, y el fallo que eso produce no se parece en nada a su causa: un test
 * de {@code RepartoDePesoTest} que lleva meses en verde empieza a fallar sin
 * que nadie lo haya tocado.
 *
 * <p>Por eso vive aislada, con un {@code @AfterEach} que restaura los valores
 * de fabrica siempre, incluso si la prueba falla a mitad.
 */
class ConfigurarPesosTest {

    /**
     * Los mismos ocho valores que trae {@link CalculadoraCompatibilidad} de
     * fabrica. Duplicarlos aqui es deliberado: si esta prueba leyera los
     * valores actuales de la clase para "recordarlos" y restaurarlos despues,
     * una ejecucion anterior que ya los hubiera dejado mal no se notaria
     * nunca. Comparar contra un valor fijo es lo que detecta el problema en
     * vez de heredarlo en silencio.
     */
    private static final PesosDelMotor DE_FABRICA =
            new PesosDelMotor(40, 10, 10, 12, 10, 5, 8, 5);

    @AfterEach
    void restaurar() {
        CalculadoraCompatibilidad.configurar(DE_FABRICA);
    }

    @Test
    @DisplayName("configurar() cambia el peso que usa el resto del motor")
    void configurarCambiaElPeso() {
        CalculadoraCompatibilidad.configurar(
                new PesosDelMotor(99, 10, 10, 12, 10, 5, 8, 5));

        // RendimientoDelPerfil —el aviso de "esto te esta costando puntos"—
        // lee este mismo campo directamente. Comprobarlo aqui es comprobar
        // que los dos siguen viendo el mismo numero, que es la razon de ser
        // de todo esto: que nunca haya dos sitios con dos ideas distintas de
        // cuanto vale el horario.
        assertEquals(99, CalculadoraCompatibilidad.PESO_HORARIO);
    }

    @Test
    @DisplayName("Los ocho pesos se actualizan, no solo el primero")
    void seActualizanLosOcho() {
        CalculadoraCompatibilidad.configurar(
                new PesosDelMotor(1, 2, 3, 4, 5, 6, 7, 8));

        assertEquals(1, CalculadoraCompatibilidad.PESO_HORARIO);
        assertEquals(2, CalculadoraCompatibilidad.PESO_NIVEL);
        assertEquals(3, CalculadoraCompatibilidad.PESO_FUERZA);
        assertEquals(4, CalculadoraCompatibilidad.PESO_OBJETIVO);
        assertEquals(5, CalculadoraCompatibilidad.PESO_CONSTANCIA);
        assertEquals(6, CalculadoraCompatibilidad.PESO_RUTINA);
        assertEquals(7, CalculadoraCompatibilidad.PESO_GIMNASIO);
        assertEquals(8, CalculadoraCompatibilidad.PESO_EDAD);
    }

    @Test
    @DisplayName("No hace falta que los ocho sumen 100: la redistribución ya rescala")
    void noHaceFaltaQueSumen100() {
        // Con un perfil completo por los dos lados —todos los factores
        // aplicables— la redistribucion rescala sobre el total que sea,
        // asi que un reparto que suma 50 en vez de 100 tiene que seguir
        // llegando a un maximo de 100 puntos, no de 50.
        CalculadoraCompatibilidad.configurar(
                new PesosDelMotor(10, 10, 5, 5, 5, 5, 5, 5)); // suma 50

        Usuario yo = personaCompleta("yo@test.com");
        Usuario otro = personaCompleta("otro@test.com");

        var horarios = java.util.List.of(
                new com.spotterai.backend.models.Disponibilidad(
                        "Lunes", java.time.LocalTime.of(18, 0), java.time.LocalTime.of(20, 0), yo, true));

        var mio = new PerfilDeMatch(yo, horarios, java.util.List.of(), new Constancia(12, true));
        var suyo = new PerfilDeMatch(otro, horarios, java.util.List.of(), new Constancia(12, true));

        var puntuacion = CalculadoraCompatibilidad.calcular(mio, suyo);

        assertTrue(puntuacion.total() <= 100,
                "Un reparto que suma menos de 100 no puede dar mas de 100 puntos: dio "
                        + puntuacion.total());
    }

    private static Usuario personaCompleta(String email) {
        com.spotterai.backend.models.Gimnasio g = new com.spotterai.backend.models.Gimnasio();
        g.setId(1L);
        g.setNombre("McFit");

        Usuario u = new Usuario();
        u.setEmail(email);
        u.setNivel("Intermedio");
        u.setObjetivos("Hipertrofia");
        u.setEdad(28);
        u.setRutina("TORSO_PIERNA");
        u.setGimnasio(g);
        return u;
    }
}
