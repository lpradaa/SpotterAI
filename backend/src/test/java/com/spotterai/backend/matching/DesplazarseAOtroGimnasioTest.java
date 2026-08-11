package com.spotterai.backend.matching;

import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.models.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Entrenar juntos viniendo de gimnasios distintos.
 *
 * <p>Coincidir a la misma hora en dos edificios distintos no es coincidir, y por
 * eso el solape se multiplica por 0,25. Eso sigue siendo verdad. Lo que no lo
 * era es aplicarselo a todo el mundo por igual: quien esta dispuesto a coger el
 * metro tres paradas puntuaba exactamente como quien no piensa moverse, porque
 * la aplicacion nunca lo preguntaba.
 *
 * <p>Lo que se comprueba aqui es que el dato sirva de algo <b>sin</b> llegar a
 * borrar la diferencia con compartir gimnasio, que es la trampa evidente: si
 * desplazarse empatara con entrenar en la misma sala, el factor dejaria de
 * distinguir nada y bastaria con marcar la casilla para subir en toda la lista.
 */
class DesplazarseAOtroGimnasioTest {

    private static Gimnasio gimnasio(Long id, String nombre) {
        Gimnasio g = new Gimnasio();
        g.setId(id);
        g.setNombre(nombre);
        return g;
    }

    private static Disponibilidad franja(String dia, int desde, int hasta) {
        Disponibilidad d = new Disponibilidad();
        d.setDiaSemana(dia);
        d.setHoraInicio(LocalTime.of(desde, 0));
        d.setHoraFin(LocalTime.of(hasta, 0));
        return d;
    }

    /** Dos perfiles identicos salvo por el gimnasio y por si se mueven. */
    private static PerfilDeMatch perfil(Gimnasio gimnasio, boolean seDesplaza) {
        Usuario u = new Usuario();
        u.setNombre("Test");
        u.setNivel("Intermedio");
        u.setObjetivos("Hipertrofia");
        u.setEdad(28);
        u.setRutina(Rutina.TORSO_PIERNA.name());
        u.setGimnasio(gimnasio);
        u.setPuedoDesplazarme(seDesplaza);

        return new PerfilDeMatch(u,
                List.of(franja("Lunes", 18, 20), franja("Miércoles", 18, 20)),
                List.of(),
                new Constancia(12, true));
    }

    private static int puntuacion(PerfilDeMatch a, PerfilDeMatch b) {
        return CalculadoraCompatibilidad.calcular(a, b).total();
    }

    private static FactorCompatibilidad factor(PerfilDeMatch a, PerfilDeMatch b, String nombre) {
        return CalculadoraCompatibilidad.calcular(a, b).factores().stream()
                .filter(f -> f.nombre().equals(nombre))
                .findFirst().orElseThrow();
    }

    private final Gimnasio mcfit = gimnasio(1L, "McFit");
    private final Gimnasio basicFit = gimnasio(2L, "Basic-Fit");

    @Test
    @DisplayName("Poder desplazarse mejora la puntuacion con gimnasios distintos")
    void desplazarseSuma() {
        int sinMoverse = puntuacion(perfil(mcfit, false), perfil(basicFit, false));
        int moviendose = puntuacion(perfil(mcfit, true), perfil(basicFit, false));

        assertTrue(moviendose > sinMoverse,
                "Con alguien dispuesto a desplazarse deberia puntuar mas alto: "
                        + moviendose + " vs " + sinMoverse);
    }

    @Test
    @DisplayName("Basta con que lo diga uno de los dos")
    void bastaConUno() {
        int loDigoYo = puntuacion(perfil(mcfit, true), perfil(basicFit, false));
        int loDiceEl = puntuacion(perfil(mcfit, false), perfil(basicFit, true));
        int loDicenLosDos = puntuacion(perfil(mcfit, true), perfil(basicFit, true));

        // Para que la pareja funcione solo hace falta que se mueva una persona,
        // asi que los tres casos valen lo mismo. Exigirlo a los dos dejaria fuera
        // el caso mas normal: uno con el gimnasio al lado y otro dispuesto a ir.
        assertEquals(loDigoYo, loDiceEl);
        assertEquals(loDigoYo, loDicenLosDos);
    }

    @Test
    @DisplayName("Desplazarse nunca empata con compartir gimnasio")
    void nuncaEmpataConCompartirSala() {
        int mismoGimnasio = puntuacion(perfil(mcfit, false), perfil(mcfit, false));
        int distintoPeroMeMuevo = puntuacion(perfil(mcfit, true), perfil(basicFit, true));

        assertTrue(distintoPeroMeMuevo < mismoGimnasio,
                "Desplazarse cuesta tiempo y a menudo una entrada: no puede empatar "
                        + "con entrenar en la misma sala. " + distintoPeroMeMuevo
                        + " vs " + mismoGimnasio);
    }

    @Test
    @DisplayName("El factor gimnasio sigue dando cero: no comparten gimnasio")
    void elFactorGimnasioNoSeRegala() {
        FactorCompatibilidad g = factor(perfil(mcfit, true), perfil(basicFit, true), "gimnasio");

        // Estar dispuesto a viajar no hace que compartan gimnasio. Lo que cambia
        // es lo que significa coincidir en horario, y eso se pondera en el otro.
        assertEquals(0, g.puntos());
        assertTrue(g.aplicable());
        assertTrue(g.detalle().contains("puede desplazarse"));
    }

    @Test
    @DisplayName("Compartiendo gimnasio, marcarlo no cambia nada")
    void compartiendoGimnasioNoSuma() {
        int sinMarcar = puntuacion(perfil(mcfit, false), perfil(mcfit, false));
        int marcado = puntuacion(perfil(mcfit, true), perfil(mcfit, true));

        // No hay nada que resolver: ya entrenan en el mismo sitio.
        assertEquals(sinMarcar, marcado);
    }

    @Test
    @DisplayName("La explicacion del horario dice que hay salida")
    void laExplicacionLoCuenta() {
        FactorCompatibilidad h = factor(perfil(mcfit, true), perfil(basicFit, false), "horario");

        assertTrue(h.detalle().contains("gimnasios distintos"));
        assertTrue(h.detalle().contains("desplazarse"));
    }
}
