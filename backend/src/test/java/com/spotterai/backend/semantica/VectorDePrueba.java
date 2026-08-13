package com.spotterai.backend.semantica;

import com.spotterai.backend.models.Usuario;

/**
 * Una biografia con su vector, para las pruebas que necesitan un perfil
 * completo.
 *
 * <p>Desde que la afinidad de lo escrito es el noveno factor, "perfil completo"
 * incluye haber escrito una biografia. Varias pruebas que construyen perfiles
 * completos —para comprobar que no hay descuento por evidencia, o que ningun
 * factor se queda fuera— empezaron a fallar por esto, y la respuesta correcta no
 * era relajar esas comprobaciones: era darle biografia a los perfiles que dicen
 * estar completos.
 *
 * <p>El vector se fabrica aqui en vez de llamar al modelo: una prueba unitaria
 * no debe depender de que un servicio externo este levantado, y lo que esas
 * pruebas comprueban es el reparto de pesos, no la calidad del embedding. Que el
 * modelo distinga bien se mide aparte, en {@code embeddings/calibracion}.
 */
public final class VectorDePrueba {

    private VectorDePrueba() {}

    /** Le pone al usuario una biografia y un vector coherente con ella. */
    public static Usuario con(Usuario usuario, double semilla) {
        usuario.setBiografia("Biografía de prueba " + semilla);
        usuario.setBiografiaVector(vector(semilla).aBytes());
        usuario.setBiografiaVectorDe("huella-" + semilla);
        return usuario;
    }

    /** El mismo vector para los dos: dos personas que se describen igual. */
    public static void aLosDos(Usuario uno, Usuario otro) {
        con(uno, 1.0);
        con(otro, 1.0);
    }

    /** Deterministas y normalizados, como los que devuelve el servicio real. */
    public static VectorDeTexto vector(double semilla) {
        float[] v = new float[VectorDeTexto.DIMENSIONES];
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) Math.sin(semilla + i * 0.1);
        }

        double suma = 0;
        for (float x : v) suma += x * x;
        float norma = (float) Math.sqrt(suma);
        for (int i = 0; i < v.length; i++) v[i] /= norma;

        return new VectorDeTexto(v);
    }
}
