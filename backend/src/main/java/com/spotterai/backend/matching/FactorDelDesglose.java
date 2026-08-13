package com.spotterai.backend.matching;

import com.spotterai.backend.textos.Textos;
import com.spotterai.backend.textos.Mensaje;
import java.util.Map;

/**
 * Un factor, tal y como se enseña.
 *
 * <p>Es {@link FactorCompatibilidad} con dos cosas añadidas y una quitada: se le
 * pone una etiqueta legible y los puntos redondeados, y se deja fuera todo lo
 * que solo le sirve al calculo. La calculadora habla de "objetivo" y de 12,857
 * puntos; una pantalla tiene que decir "Qué buscáis" y 13.
 *
 * <p>Existe porque el desglose ya se calculaba entero y no salia de la base: los
 * ocho factores se aplastaban a una sola cadena unida por ". " y al DTO llegaba
 * la frase del factor dominante, uno de ocho. La tesis del producto es que un
 * porcentaje sin explicacion no vale nada, y lo que se servia era un porcentaje
 * con una frase.
 *
 * @param nombre    identificador estable, el que usa la calculadora
 * @param etiqueta  como se llama en pantalla
 * @param puntos    puntos obtenidos, redondeados
 * @param puntosMax peso efectivo del factor tras repartir el de los no aplicables
 * @param aplicable si habia datos en los dos perfiles para evaluarlo
 * @param detalle   la frase que ya redactaba la calculadora
 */
public record FactorDelDesglose(
        String nombre,
        String etiqueta,
        int puntos,
        int puntosMax,
        boolean aplicable,
        String detalle) {

    /**
     * Como se llama cada factor en pantalla.
     *
     * <p>Las claves son las que usa {@link CalculadoraCompatibilidad}; si alguna
     * dejara de coincidir, {@link #etiquetaDe} devuelve el propio nombre en vez
     * de romperse, que en una pantalla de apoyo es mejor que un hueco.
     */
    private static final Map<String, String> ETIQUETAS = Map.of(
            "horario", "Cuándo entrenáis",
            "nivel", "Nivel",
            "fuerza", "Lo que movéis",
            "objetivo", "Qué buscáis",
            "constancia", "Constancia",
            "rutina", "Cómo repartís la semana",
            "gimnasio", "Dónde entrenáis",
            "edad", "Edad");

    /*
     * Aqui llegue a poner un mapa de "que tienes que rellenar tu" por factor,
     * para enlazar al campo que falta. Se ha quitado: la calculadora tiene los
     * dos perfiles delante y aun asi escribe "falta el nivel de alguno de los
     * dos", sin decir de quien, y esa vaguedad es deliberada. Decirle a alguien
     * "completa tus levantamientos" cuando los que faltan son los del otro seria
     * afirmar algo que este lado no sabe, que es justo lo que el motor evita al
     * distinguir "no aplicable" de "cero puntos". La frase del detalle ya lo
     * cuenta con la precision que hay.
     */

    static String etiquetaDe(String nombre) {
        return ETIQUETAS.getOrDefault(nombre, nombre);
    }

    /**
     * Pasa un factor del calculo a factor de pantalla.
     *
     * <p>Los puntos se redondean aqui y no en el navegador para que el desglose
     * y el total no puedan discrepar por donde se redondee cada uno.
     */
    /**
     * @param textos quien redacta, con el idioma de quien ha preguntado. El
     *               factor llega con claves; aqui es donde se convierten en
     *               frases, que es el ultimo momento en que se puede saber el
     *               idioma sin arrastrarlo por todo el motor.
     */
    public static FactorDelDesglose de(FactorCompatibilidad factor, Textos textos) {
        return new FactorDelDesglose(
                factor.nombre(),
                textos.de(Mensaje.de("factor.nombre." + factor.nombre())),
                (int) Math.round(factor.puntos()),
                (int) Math.round(factor.puntosMax()),
                factor.aplicable(),
                textos.de(factor.detalle()));
    }
}
