package com.spotterai.backend.matching;

import com.spotterai.backend.textos.Textos;
import com.spotterai.backend.textos.Mensaje;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Convierte una puntuacion de compatibilidad en una explicacion que una persona
 * quiera leer.
 *
 * <p>Hila los textos que ya trae cada factor, sin inventar nada: la puntuacion la
 * calcula {@link CalculadoraCompatibilidad} y aqui solo se redacta, de modo que
 * la explicacion nunca puede contradecir al numero que acompaña.
 *
 * <p>Hubo una version que pasaba este mismo desglose por la API de Claude para
 * darle mejor prosa. Se retiro sin que cambiara nada en pantalla, porque sin
 * clave configurada lo que se veia ya era este texto. El codigo y el porque de
 * la decision estan en {@code docs/ia-aparcada/}.
 */
@Service
public class ExplicadorCompatibilidad {

    private final Textos textos;

    public ExplicadorCompatibilidad(Textos textos) {
        this.textos = textos;
    }

    public ExplicacionMatch explicar(String nombreOtro, PuntuacionCompatibilidad puntuacion) {
        String motivo = puntuacion.factores().stream()
                // Un factor a cero no es un motivo, y uno sin datos tampoco: colar
                // "faltan los horarios" entre las razones de un match lo empeora.
                .filter(f -> f.puntos() > 0)
                .map(f -> textos.de(f.detalle()))
                .collect(Collectors.joining(". "));

        if (motivo.isBlank()) {
            motivo = textos.de(Mensaje.de("compat.sinPuntosEnComun"));
        } else {
            motivo += ".";
        }

        // El desglose entero, en el orden en que lo dejo la calculadora: primero
        // lo que mas pesa. Se mandan tambien los no aplicables, que es justo lo
        // que permite decir "esto no lo sabemos" en vez de enseñar un cero que
        // se leeria como mal encaje.
        List<FactorDelDesglose> factores = puntuacion.factores().stream()
                .map(f -> FactorDelDesglose.de(f, textos))
                .toList();

        return new ExplicacionMatch(
                textos.de(Mensaje.de("compat.titular", nombreOtro, puntuacion.total())),
                motivo,
                puntuacion.total(),
                textos.de(puntuacion.etiqueta()),
                factores);
    }
}
