package com.spotterai.backend.matching;

import java.util.List;

/**
 * Resultado completo del calculo de compatibilidad entre dos usuarios.
 *
 * <p>Guarda el desglose entero, no solo el numero: la explicacion que ve el usuario
 * se genera a partir de los factores, asi que nunca puede contradecir la puntuacion.
 *
 * @param total   puntuacion de 0 a 100
 * @param factores desglose por factor, en orden de peso descendente
 * @param solape  detalle del cruce de horarios
 */
public record PuntuacionCompatibilidad(int total, List<FactorCompatibilidad> factores, SolapeHorario solape) {

    /** Etiqueta corta para la interfaz. */
    public String etiqueta() {
        if (total >= 85) return "Compatibilidad excelente";
        if (total >= 70) return "Muy compatibles";
        if (total >= 50) return "Buena compatibilidad";
        if (total >= 30) return "Compatibilidad parcial";
        return "Poca compatibilidad";
    }

    /** El factor que mas ha aportado en terminos absolutos. Util para titulares. */
    public FactorCompatibilidad factorDominante() {
        return factores.stream()
                .max(java.util.Comparator.comparingDouble(FactorCompatibilidad::puntos))
                .orElseThrow(() -> new IllegalStateException("Una puntuacion siempre tiene factores"));
    }
}
