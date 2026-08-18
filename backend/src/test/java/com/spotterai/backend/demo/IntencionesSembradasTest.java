package com.spotterai.backend.demo;

import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.semantica.IntencionesDeBiografia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los tres ejes que van dentro de la demo.
 *
 * <p>Mismo papel que {@link VectoresSembradosTest}: existen para que un
 * despliegue pueda correr sin levantar el servicio del modelo, y un fichero de
 * datos generado a mano se pudre en silencio. Alguien cambia un texto de
 * demostracion, nadie regenera esto, y la demo empareja a la gente por frases
 * que ya no dice.
 *
 * <p>Con las intenciones importa mas que con los vectores, porque desde que el
 * servicio lee ejes ocupa 611 MB en vez de 475: la opcion de montar la demo sin
 * el modelo vale mas que antes.
 */
class IntencionesSembradasTest {

    private final IntencionesSembradas intenciones = new IntencionesSembradas();

    private static Usuario alex() {
        Usuario u = new Usuario();
        u.setEmail("demo@spotterai.test");
        u.setBiografia("Llevo dos años entrenando en serio. "
                + "Busco a alguien constante para los días de fuerza.");
        return u;
    }

    @Test
    @DisplayName("el fichero está y trae los trece")
    void estanTodos() {
        assertThat(intenciones.cuantos()).isEqualTo(13);
    }

    @Test
    @DisplayName("los ejes llegan a la entidad, con su huella")
    void seAplican() {
        Usuario u = alex();
        intenciones.aplicarA(u);

        // Alex habla de los tres, asi que es el caso completo.
        assertThat(u.getIntencionExigencia()).isNotNull();
        assertThat(u.getIntencionAmbicion()).isNotNull();
        assertThat(u.getIntencionFlexibilidad()).isNotNull();

        // La huella sale de la biografia real, no del fichero: si alguien edita
        // el texto sin regenerar esto, deja de cuadrar y el repaso lo detecta.
        assertThat(u.getIntencionesDe())
                .isEqualTo(IntencionesDeBiografia.huellaDe(u.getBiografia()));
    }

    /**
     * El hueco vacio tiene que llegar como null y no como cero.
     *
     * <p>Es la mitad de las celdas del fichero. Un cero colocaria a esa persona
     * justo en el medio del eje —una postura que no ha dado— y el factor la
     * compararia con todo el mundo en un eje del que no habló.
     */
    @Test
    @DisplayName("un eje vacío en el fichero llega como «no ha dicho nada»")
    void elHuecoNoEsUnCero() throws IOException {
        Path fichero = Paths.get("src", "main", "resources", "demo", "intenciones-biografia.tsv");
        List<String> lineas = Files.readAllLines(fichero, StandardCharsets.UTF_8);

        long conHueco = lineas.stream()
                .filter(l -> !l.isBlank())
                .filter(l -> {
                    String[] p = l.split("\t", -1);
                    return p.length >= 4
                            && (p[1].isBlank() || p[2].isBlank() || p[3].isBlank());
                })
                .count();

        assertTrue(conHueco > 0,
                "Si ninguna biografía de la demo deja un eje vacío, o el fichero está mal "
                        + "generado o el umbral del servicio se ha ido a cero: media biografía "
                        + "real no habla de la mitad de los ejes");

        // Y una que lo tiene, comprobada de punta a punta.
        Usuario diego = new Usuario();
        diego.setEmail("diego@spotterai.test");
        diego.setBiografia("Empuje/tirón/pierna. Entreno temprano casi siempre.");
        intenciones.aplicarA(diego);

        // Habla de rutina y de horario, que ya son campos del perfil: de los
        // tres ejes no dice practicamente nada.
        assertThat(diego.getIntencionExigencia()).isNull();
        assertThat(diego.getIntencionAmbicion()).isNull();
    }

    @Test
    @DisplayName("quien no está en el fichero se queda sin ejes, no con ceros")
    void aQuienNoEstaNoSeLeInventaNada() {
        Usuario desconocido = new Usuario();
        desconocido.setEmail("nadie@spotterai.test");
        desconocido.setBiografia("Una biografía que no está sembrada.");

        intenciones.aplicarA(desconocido);

        assertThat(desconocido.getIntencionExigencia()).isNull();
        assertThat(desconocido.getIntencionesDe()).isNull();
    }
}
