package com.spotterai.backend.matching;

import java.util.List;

/**
 * Explicacion de por que dos usuarios encajan.
 *
 * <p>La redacta {@link ExplicadorCompatibilidad} a partir del desglose ya
 * puntuado, y viaja al frontend tal cual.
 *
 * <p>Antes solo llevaba las dos frases. El desglose se calculaba entero —cada
 * factor con sus puntos, su maximo y su explicacion— y se tiraba al unirlo todo
 * en una cadena, asi que la pantalla podia decir "78 %" y una frase, pero no de
 * donde salia ese 78. Ahora viajan los ocho factores, y con ellos las dos
 * decisiones que mas cuestan del motor y de las que no habia rastro en pantalla:
 * que un factor sin datos no puntue como un cero, y que su peso se reparta entre
 * los demas en vez de perderse.
 *
 * @param titular  frase corta para la cabecera de la tarjeta
 * @param motivo   dos o tres frases explicando la compatibilidad
 * @param total    la puntuacion, para que el desglose y el numero no se separen
 * @param etiqueta "Muy compatibles", "Compatibilidad parcial"...
 * @param factores el desglose entero, incluidos los que no se han podido evaluar
 */
public record ExplicacionMatch(
        String titular,
        String motivo,
        int total,
        String etiqueta,
        List<FactorDelDesglose> factores) {

    public ExplicacionMatch {
        factores = List.copyOf(factores);
    }
}
