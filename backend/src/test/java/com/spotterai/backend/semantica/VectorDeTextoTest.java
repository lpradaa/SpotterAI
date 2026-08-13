package com.spotterai.backend.semantica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El formato del vector y la similitud.
 *
 * <p>Esta clase es el unico sitio que escribe y lee los bytes que van a la base,
 * asi que lo que se fija aqui es que lo guardado y lo leido sean lo mismo. Un
 * fallo de un bit en este ida y vuelta no da un error: da similitudes que no
 * significan nada, repartidas por todo el motor y sin nada que las delate.
 */
class VectorDeTextoTest {

    private static float[] valoresDe(double semilla) {
        float[] v = new float[VectorDeTexto.DIMENSIONES];
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) Math.sin(semilla + i);
        }
        return normalizar(v);
    }

    /** Longitud 1, que es como los devuelve el servicio de embeddings. */
    private static float[] normalizar(float[] v) {
        double suma = 0;
        for (float x : v) suma += x * x;
        double norma = Math.sqrt(suma);
        for (int i = 0; i < v.length; i++) v[i] /= (float) norma;
        return v;
    }

    // ===================== El ida y vuelta =====================

    @Test
    @DisplayName("lo que se guarda es lo que se lee")
    void idaYVuelta() {
        VectorDeTexto original = new VectorDeTexto(valoresDe(0.5));

        VectorDeTexto recuperado = VectorDeTexto.desdeBytes(original.aBytes());

        assertThat(recuperado).isNotNull();
        assertThat(recuperado.valores()).containsExactly(original.valores());
    }

    @Test
    @DisplayName("un vector ocupa exactamente lo que dice la migración")
    void tamanoEnBytes() {
        // 384 floats de 4 bytes. Si esto cambia, la columna de la base tambien
        // tiene que cambiar, y conviene que lo diga una prueba y no una fila
        // corrupta.
        assertThat(new VectorDeTexto(valoresDe(1)).aBytes()).hasSize(1536);
    }

    @Test
    @DisplayName("de una lista de decimales sale un vector")
    void desdeLaRespuestaDelServicio() {
        List<Double> numeros = new java.util.ArrayList<>();
        for (float valor : valoresDe(2)) numeros.add((double) valor);

        assertThat(VectorDeTexto.de(numeros).valores()).hasSize(VectorDeTexto.DIMENSIONES);
    }

    // ===================== Lo que no cuadra =====================

    @Test
    @DisplayName("un vector de otra dimensión no se construye")
    void dimensionEquivocada() {
        assertThatThrownBy(() -> new VectorDeTexto(new float[10]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("384");
    }

    @Test
    @DisplayName("unos bytes que no cuadran se leen como «no hay vector», no como un error")
    void bytesDeOtroModelo() {
        // Una fila guardada por un modelo anterior. Lo correcto es tratarla como
        // dato ausente —que el motor sabe repartir— y no tumbar el calculo de
        // compatibilidad de todo el mundo.
        assertThat(VectorDeTexto.desdeBytes(new byte[]{1, 2, 3})).isNull();
        assertThat(VectorDeTexto.desdeBytes(null)).isNull();
    }

    // ===================== La similitud =====================

    @Test
    @DisplayName("un texto es idéntico a sí mismo")
    void identico() {
        VectorDeTexto v = new VectorDeTexto(valoresDe(3));

        assertThat(v.similitudCon(v)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-5));
    }

    @Test
    @DisplayName("dos textos distintos se parecen menos que uno consigo mismo")
    void distintos() {
        VectorDeTexto uno = new VectorDeTexto(valoresDe(0));
        VectorDeTexto otro = new VectorDeTexto(valoresDe(7));

        assertThat(uno.similitudCon(otro)).isLessThan(uno.similitudCon(uno));
    }

    @Test
    @DisplayName("la similitud es simétrica")
    void simetrica() {
        VectorDeTexto uno = new VectorDeTexto(valoresDe(1));
        VectorDeTexto otro = new VectorDeTexto(valoresDe(4));

        // Que A se parezca a B lo mismo que B a A no es una obviedad del
        // producto escalar: es lo que garantiza que la compatibilidad siga
        // saliendo igual mires a quien mires, que es una promesa del motor.
        assertThat(uno.similitudCon(otro))
                .isCloseTo(otro.similitudCon(uno), org.assertj.core.data.Offset.offset(1e-6));
    }
}
