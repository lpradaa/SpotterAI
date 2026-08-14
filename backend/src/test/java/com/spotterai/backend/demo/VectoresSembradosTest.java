package com.spotterai.backend.demo;

import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.semantica.VectorDeBiografia;
import com.spotterai.backend.semantica.VectorDeTexto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los vectores que van dentro de la demo.
 *
 * <p>Existen para que un despliegue pueda correr sin levantar el servicio de
 * embeddings: las biografias de demostracion llevan su vector ya calculado, y
 * sin ellos el factor semantico saldria "sin datos" para todo el mundo justo en
 * la instancia que existe para enseñarlo.
 *
 * <p>Un fichero de datos generado a mano se pudre en silencio: alguien cambia un
 * texto de demostracion, nadie regenera los vectores, y la demo se queda
 * comparando a la gente por frases que ya no dice. Esto lo vigila.
 */
class VectoresSembradosTest {

    private final VectoresSembrados vectores = new VectoresSembrados();

    @Test
    @DisplayName("el fichero está y trae los trece")
    void estanTodos() {
        assertThat(vectores.cuantos()).isEqualTo(13);
    }

    @Test
    @DisplayName("cada vector se lee como los 384 números que espera el motor")
    void sonVectoresValidos() {
        Usuario u = new Usuario();
        u.setEmail("marta@spotterai.test");
        u.setBiografia("Torso/pierna cuatro días. Prefiero entrenar acompañada en los básicos.");

        vectores.aplicarA(u);

        VectorDeTexto v = VectorDeTexto.desdeBytes(u.getBiografiaVector());
        assertThat(v).isNotNull();

        // Normalizado a longitud 1, que es de lo que depende que la similitud
        // sea un producto escalar a secas. Un vector sin normalizar daria
        // similitudes fuera de escala sin que nada avisara.
        assertThat(v.similitudCon(v)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-3));
    }

    @Test
    @DisplayName("la huella corresponde a la biografía, no viene del fichero")
    void laHuellaSaleDelTexto() {
        String bio = "Vengo de powerlifting. Necesito a alguien que pueda ayudarme en banca pesada.";
        Usuario u = new Usuario();
        u.setEmail("javi@spotterai.test");
        u.setBiografia(bio);

        vectores.aplicarA(u);

        // Que la huella se calcule del texto y no venga en el fichero es lo que
        // hace que cambiar una biografia de demostracion sin regenerar los
        // vectores se detecte: la huella deja de cuadrar y el repaso trata ese
        // vector como desfasado en vez de darlo por bueno.
        assertThat(u.getBiografiaVectorDe()).isEqualTo(VectorDeBiografia.huellaDe(bio));
    }

    @Test
    @DisplayName("a quien no está en el fichero no se le inventa un vector")
    void desconocido() {
        Usuario u = new Usuario();
        u.setEmail("nadie@spotterai.test");
        u.setBiografia("Alguien que no es de la demo");

        vectores.aplicarA(u);

        assertThat(u.getBiografiaVector()).isNull();
    }

    @Test
    @DisplayName("sin biografía no se le pone vector a nadie")
    void sinBiografia() {
        Usuario u = new Usuario();
        u.setEmail("marta@spotterai.test");

        vectores.aplicarA(u);

        assertThat(u.getBiografiaVector()).isNull();
    }
}
