package com.spotterai.backend.matching;

import java.util.List;

/**
 * Resultado de cruzar los horarios de dos usuarios.
 *
 * @param minutosSemanales minutos totales que ambos coinciden en el gimnasio a lo largo de la semana
 * @param dias             dias de la semana con al menos un minuto en comun, en orden natural
 * @param franjas          descripcion legible de cada coincidencia ("Martes 19:00-21:00")
 */
public record SolapeHorario(int minutosSemanales, List<String> dias, List<String> franjas) {

    public static final SolapeHorario NINGUNO = new SolapeHorario(0, List.of(), List.of());

    public boolean hayCoincidencia() {
        return minutosSemanales > 0;
    }
}
