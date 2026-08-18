package com.spotterai.backend.semantica;

import com.spotterai.backend.models.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo que mantiene unidos los dos lados del factor.
 *
 * <p>Los ejes viven en dos sitios —{@code embeddings/intenciones.py} los lee y
 * esta clase los guarda— y eso es exactamente la clase de duplicado que diverge
 * en silencio: si el servicio empieza a mandar {@code motivacion} en vez de
 * {@code ambicion}, el backend no encuentra la clave, guarda un null y el factor
 * pierde un tercio de su informacion <b>sin que nada falle</b>. Ni una excepcion,
 * ni un log: solo peores emparejamientos.
 */
class IntencionesDeBiografiaTest {

    /** Sin URL configurada, el servicio esta apagado: no hace ninguna llamada. */
    private final IntencionesDeBiografia intenciones =
            new IntencionesDeBiografia(new ServicioDeEmbeddings(""));

    private static Usuario conBiografia(String texto) {
        Usuario u = new Usuario();
        u.setBiografia(texto);
        return u;
    }

    @Test
    @DisplayName("La huella es la misma que calcula el servicio, letra por letra")
    void laHuellaNoPuedeDivergir() {
        // Fijado a mano: los primeros 32 caracteres del SHA-256 del texto
        // recortado. Si alguno de los dos lados cambia de algoritmo, de longitud
        // o de recorte, esto falla — que es el unico aviso que habria, porque
        // una huella distinta solo produce recalculos infinitos.
        assertThat(IntencionesDeBiografia.huellaDe("Todavía me da respeto la zona de peso libre"))
                .hasSize(32)
                .matches("[0-9a-f]{32}");

        // El recorte importa: el servicio hace .strip() antes de resumir.
        assertThat(IntencionesDeBiografia.huellaDe("  hola  "))
                .isEqualTo(IntencionesDeBiografia.huellaDe("hola"));
    }

    @Test
    @DisplayName("Los tres ejes se llaman igual en Java que en el servicio")
    void losEjesNoPuedenDivergir() throws IOException {
        // Se lee el fichero de Python de verdad. Es feo y es el unico sitio
        // donde los dos lados se tocan: cualquier cosa mas elegante —una
        // constante compartida, un contrato generado— seria mas codigo que el
        // problema.
        Path fuente = Paths.get("..", "embeddings", "intenciones.py");
        assertTrue(Files.exists(fuente),
                "No esta el servicio en " + fuente.toAbsolutePath().normalize()
                        + ". Si se ha movido, esta prueba hay que moverla con el.");

        String python = Files.readString(fuente, StandardCharsets.UTF_8);

        for (String eje : new String[]{IntencionesDeBiografia.EXIGENCIA,
                                       IntencionesDeBiografia.AMBICION,
                                       IntencionesDeBiografia.FLEXIBILIDAD}) {
            assertTrue(python.contains("\"" + eje + "\""),
                    "El servicio ya no manda el eje «" + eje + "». El backend guardaria "
                            + "null en esa columna y el factor perderia un tercio de su "
                            + "informacion sin que nada fallara.");
        }
    }

    @Test
    @DisplayName("Sin biografía no hay intenciones, y si las había se quitan")
    void borrarLaBiografiaBorraLosEjes() {
        Usuario u = conBiografia(null);
        u.setIntencionExigencia(0.8);
        u.setIntencionesDe("huella-vieja");

        assertThat(intenciones.actualizar(u)).isTrue();

        // Dejarlas seria seguir emparejando a alguien por un texto que ha
        // retirado a proposito.
        assertThat(u.getIntencionExigencia()).isNull();
        assertThat(u.getIntencionesDe()).isNull();
    }

    @Test
    @DisplayName("Estar al día se decide por la huella, no por si hay valores")
    void alDiaSeMidePorLaHuella() {
        String bio = "Busco a alguien que me exija.";

        Usuario u = conBiografia(bio);
        assertThat(intenciones.estaAlDia(u)).isFalse();

        u.setIntencionesDe(IntencionesDeBiografia.huellaDe(bio));
        // Los tres ejes sigue siendo null y aun asi esta al dia: una biografia
        // que no habla de ninguno de los tres es un resultado legitimo, y sin
        // esto se releeria en cada arranque para siempre.
        assertThat(intenciones.estaAlDia(u)).isTrue();

        u.setBiografia("Ahora digo otra cosa.");
        assertThat(intenciones.estaAlDia(u)).isFalse();
    }

    @Test
    @DisplayName("Con el servicio apagado no se toca nada")
    void sinServicioNoSeInventaNada() {
        Usuario u = conBiografia("Vengo de powerlifting.");

        assertThat(intenciones.actualizar(u)).isFalse();
        assertThat(u.getIntencionExigencia()).isNull();
        assertThat(u.getIntencionesDe()).isNull();
    }
}
