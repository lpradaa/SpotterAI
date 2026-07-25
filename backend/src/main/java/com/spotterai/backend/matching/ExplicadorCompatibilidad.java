package com.spotterai.backend.matching;

import org.springframework.stereotype.Service;

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

    public ExplicacionMatch explicar(String nombreOtro, PuntuacionCompatibilidad puntuacion) {
        String motivo = puntuacion.factores().stream()
                // Un factor a cero no es un motivo, y uno sin datos tampoco: colar
                // "faltan los horarios" entre las razones de un match lo empeora.
                .filter(f -> f.puntos() > 0)
                .map(FactorCompatibilidad::detalle)
                .collect(Collectors.joining(". "));

        if (motivo.isBlank()) {
            motivo = "No hemos encontrado puntos en común claros entre vuestros perfiles.";
        } else {
            motivo += ".";
        }

        return new ExplicacionMatch(
                "%s - %d%% de compatibilidad".formatted(nombreOtro, puntuacion.total()),
                motivo);
    }
}
