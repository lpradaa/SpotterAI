package com.spotterai.backend.semantica;

import com.spotterai.backend.models.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cuando hay que recalcular el vector de una biografia.
 *
 * <p>Las dos trampas de esto son simetricas: recalcular siempre gasta una
 * llamada al modelo en cada guardado de perfil aunque nadie haya tocado su
 * biografia, y no recalcular nunca deja vectores describiendo textos que ya no
 * existen. La huella —el resumen del texto del que salio el vector— es lo que
 * separa un caso del otro.
 */
class VectorDeBiografiaTest {

    /** Sin servicio configurado: no llama a nadie, que es lo que se quiere aqui. */
    private final VectorDeBiografia vectores =
            new VectorDeBiografia(new ServicioDeEmbeddings(""));

    private static Usuario conBio(String bio) {
        Usuario u = new Usuario();
        u.setNombre("Test");
        u.setBiografia(bio);
        return u;
    }

    // ===================== La huella =====================

    @Test
    @DisplayName("la huella de Java es la misma que calcula el servicio de Python")
    void mismaHuellaEnLosDosLados() {
        // Son dos implementaciones de la misma especificacion —SHA-256 del texto
        // recortado, en hexadecimal, los 32 primeros caracteres— y eso es
        // exactamente lo que suele divergir sin que nadie se entere. Este valor
        // sale de ejecutar el servidor de embeddings real sobre este mismo
        // texto; si alguno de los dos lados se mueve, esta prueba lo dice.
        assertThat(VectorDeBiografia.huellaDe("Todavía me da respeto la zona de peso libre"))
                .isEqualTo("1d291c8310637a04ec33edd993c4ea76");
    }

    @Test
    @DisplayName("los espacios de los bordes no cambian la huella")
    void recortaAntesDeResumir() {
        assertThat(VectorDeBiografia.huellaDe("  entreno por la tarde  "))
                .isEqualTo(VectorDeBiografia.huellaDe("entreno por la tarde"));
    }

    // ===================== Cuando esta al dia =====================

    @Test
    @DisplayName("sin biografía y sin vector: no hay nada que hacer")
    void sinBiografia() {
        assertThat(vectores.estaAlDia(conBio(null))).isTrue();
        assertThat(vectores.estaAlDia(conBio("   "))).isTrue();
    }

    @Test
    @DisplayName("con biografía y sin vector: hay trabajo")
    void biografiaSinVector() {
        assertThat(vectores.estaAlDia(conBio("Entreno los lunes"))).isFalse();
    }

    @Test
    @DisplayName("el vector de la biografía actual está al día")
    void vectorQueCorresponde() {
        Usuario u = conBio("Entreno los lunes");
        u.setBiografiaVector(VectorDePrueba.vector(1).aBytes());
        u.setBiografiaVectorDe(VectorDeBiografia.huellaDe("Entreno los lunes"));

        assertThat(vectores.estaAlDia(u)).isTrue();
    }

    @Test
    @DisplayName("un vector de una biografía anterior NO está al día")
    void vectorDesfasado() {
        // Este es el caso que se escapo en la primera version y aparecio al
        // probar la degradacion: alguien edita su biografia con el servicio de
        // embeddings caido. El perfil se guarda, el vector viejo se queda, y con
        // una comprobacion que solo mirase "hay vector" ese perfil no se
        // recalcularia nunca.
        Usuario u = conBio("Ahora entreno los martes");
        u.setBiografiaVector(VectorDePrueba.vector(1).aBytes());
        u.setBiografiaVectorDe(VectorDeBiografia.huellaDe("Antes entrenaba los lunes"));

        assertThat(vectores.estaAlDia(u)).isFalse();
    }

    // ===================== Borrar la biografía =====================

    @Test
    @DisplayName("quien borra su biografía se queda sin vector")
    void borrarLaBiografiaQuitaElVector() {
        Usuario u = conBio(null);
        u.setBiografiaVector(VectorDePrueba.vector(1).aBytes());
        u.setBiografiaVectorDe("huella-vieja");

        assertThat(vectores.actualizar(u)).isTrue();

        // Dejarlo seria seguir comparando a esa persona por un texto que ha
        // decidido retirar.
        assertThat(u.getBiografiaVector()).isNull();
        assertThat(u.getBiografiaVectorDe()).isNull();
    }

    @Test
    @DisplayName("sin servicio de embeddings no se rompe nada: simplemente no hay vector")
    void sinServicio() {
        Usuario u = conBio("Entreno los lunes");

        assertThat(vectores.actualizar(u)).isFalse();
        assertThat(u.getBiografiaVector()).isNull();
    }
}
