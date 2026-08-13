package com.spotterai.backend.semantica;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Un texto convertido en numeros, y lo unico que sabe como se guardan.
 *
 * <p>El vector llega del servicio de embeddings ya normalizado a longitud 1. Eso
 * no es un detalle de formato: con dos vectores de longitud 1, la similitud del
 * coseno <em>es</em> su producto escalar, asi que {@link #similitudCon} no
 * necesita raices ni divisiones y no puede dividir por cero.
 *
 * <p>Se guarda en crudo —float de 32 bits, big-endian, 1536 bytes— y no como
 * JSON. En JSON serian unos 4 KB por persona y habria que parsearlos en cada
 * comparacion; el motor compara a una persona contra todas las demas en cada
 * calculo, asi que ese parseo estaria en el camino caliente.
 *
 * <p>Esta clase es el unico sitio que escribe y lee ese formato. Si alguna vez
 * cambia —otra dimension, otro modelo—, cambia aqui y en la migracion, no
 * repartido por el motor.
 */
public record VectorDeTexto(float[] valores) {

    /** Lo que produce el modelo multilingue del servicio de embeddings. */
    public static final int DIMENSIONES = 384;

    public VectorDeTexto {
        if (valores.length != DIMENSIONES) {
            throw new IllegalArgumentException(
                    "Un vector de biografia tiene " + DIMENSIONES + " dimensiones, no " + valores.length);
        }
    }

    public static VectorDeTexto de(List<Double> numeros) {
        float[] valores = new float[numeros.size()];
        for (int i = 0; i < numeros.size(); i++) {
            valores[i] = numeros.get(i).floatValue();
        }
        return new VectorDeTexto(valores);
    }

    /** El vector tal y como se guarda en la columna {@code biografia_vector}. */
    public byte[] aBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(DIMENSIONES * Float.BYTES).order(ByteOrder.BIG_ENDIAN);
        for (float valor : valores) {
            buffer.putFloat(valor);
        }
        return buffer.array();
    }

    /**
     * Lee un vector guardado.
     *
     * <p>Devuelve {@code null} en vez de reventar cuando los bytes no cuadran:
     * una fila con un vector de otra dimension es de un modelo anterior, y el
     * comportamiento correcto ahi es "no tenemos ese dato" —que el motor ya sabe
     * tratar— y no tumbar el calculo de compatibilidad de todo el mundo.
     */
    public static VectorDeTexto desdeBytes(byte[] bytes) {
        if (bytes == null || bytes.length != DIMENSIONES * Float.BYTES) return null;

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        float[] valores = new float[DIMENSIONES];
        for (int i = 0; i < DIMENSIONES; i++) {
            valores[i] = buffer.getFloat();
        }
        return new VectorDeTexto(valores);
    }

    /**
     * Cuanto se parecen dos textos, de -1 a 1.
     *
     * <p>Producto escalar a secas porque los dos vectores vienen normalizados.
     * En la practica, con textos del mismo dominio, los valores utiles caen
     * entre 0 y 1: dos biografias de gimnasio nunca son opuestas, solo mas o
     * menos parecidas.
     */
    public double similitudCon(VectorDeTexto otro) {
        double suma = 0;
        for (int i = 0; i < DIMENSIONES; i++) {
            suma += valores[i] * otro.valores[i];
        }
        return suma;
    }
}
