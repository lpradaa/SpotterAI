package com.spotterai.backend.matching;

/**
 * El reparto de pesos entre los ocho factores, tal y como puede venir de fuera.
 *
 * <p>Corresponde uno a uno con la tabla del README y con las constantes
 * {@code PESO_*} de {@link CalculadoraCompatibilidad}. No incluye las
 * constantes de ajuste fino dentro de un solo factor —el suelo por anclas, los
 * minutos ideales de solape, el descuento por gimnasio distinto— porque esas no
 * son lo que el embudo de compatibilidad puede decidir por si solo: son
 * calibracion de una formula, no el reparto entre factores.
 *
 * <p>No hace falta que sumen 100. La redistribucion de
 * {@code CalculadoraCompatibilidad} ya rescala en proporcion a lo que este
 * disponible, asi que lo que importa es la proporcion relativa entre los ocho,
 * no el total exacto. Es justo lo que permite probar repartos distintos sin
 * tener que cuadrar la suma a mano cada vez.
 */
public record PesosDelMotor(
        double horario,
        double nivel,
        double fuerza,
        double objetivo,
        double constancia,
        double rutina,
        double gimnasio,
        double edad) {

    public PesosDelMotor {
        // Un peso negativo no es "cuenta menos", es una resta, y el resto del
        // calculo —sobre todo la redistribucion cuando faltan datos— asume
        // pesos no negativos. Un typo con un signo de menos se descubre aqui y
        // no en una puntuacion que no cuadra.
        for (double peso : new double[]{horario, nivel, fuerza, objetivo, constancia, rutina, gimnasio, edad}) {
            if (peso < 0) {
                throw new IllegalArgumentException("Ningún peso puede ser negativo: " + peso);
            }
        }
    }
}
