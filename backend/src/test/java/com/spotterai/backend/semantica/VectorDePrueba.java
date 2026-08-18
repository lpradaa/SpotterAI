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

    /** Le pone al usuario una biografia y lo que sale de ella. */
    public static Usuario con(Usuario usuario, double semilla) {
        usuario.setBiografia("Biografía de prueba " + semilla);

        // El vector se queda mientras la columna exista, aunque desde la V19 el
        // factor no lo mire: hay pruebas que comprueban que se sigue guardando.
        usuario.setBiografiaVector(vector(semilla).aBytes());
        usuario.setBiografiaVectorDe("huella-" + semilla);

        intenciones(usuario, semilla);
        return usuario;
    }

    /**
     * Los tres ejes, deterministas a partir de la semilla.
     *
     * <p>La misma semilla da las mismas posiciones —dos personas que se
     * describen igual encajan del todo— y algunas salen <b>null</b> a
     * proposito: en las biografias reales la mitad de los ejes no aparecen, y
     * una poblacion de prueba donde todo el mundo opina de todo mediria un
     * factor que no existe.
     */
    public static void intenciones(Usuario usuario, double semilla) {
        usuario.setIntencionExigencia(eje(semilla, 0));
        usuario.setIntencionAmbicion(eje(semilla, 1));
        usuario.setIntencionFlexibilidad(eje(semilla, 2));
        usuario.setIntencionesDe("huella-" + semilla);
    }

    private static Double eje(double semilla, int indice) {
        double posicion = Math.sin(semilla * 1.7 + indice * 2.1);

        // El mismo umbral que aplica el servicio: por debajo, el texto no habla
        // de ese eje. Alla esta medido; aqui solo hace falta que algunos salgan
        // nulos, asi que se deja mas alto a proposito y la poblacion de prueba
        // sigue teniendo huecos que ejercitar.
        if (Math.abs(posicion) < 0.15) return null;
        return Math.round(posicion * 1000) / 1000.0;
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
