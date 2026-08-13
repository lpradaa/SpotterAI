package com.spotterai.backend.matching;

import com.spotterai.backend.textos.Mensaje;
import java.util.Comparator;
import java.util.List;

/**
 * Resultado completo del calculo de compatibilidad entre dos usuarios.
 *
 * <p>Guarda el desglose entero, no solo el numero: la explicacion que ve el usuario
 * se genera a partir de los factores, asi que nunca puede contradecir la puntuacion.
 *
 * @param total    puntuacion de 0 a 100
 * @param factores desglose por factor, incluidos los que no se han podido evaluar
 * @param solape   detalle del cruce de horarios
 */
public record PuntuacionCompatibilidad(int total, List<FactorCompatibilidad> factores, SolapeHorario solape) {

    public PuntuacionCompatibilidad {
        factores = List.copyOf(factores);
    }

    /** Etiqueta corta para la interfaz, sin redactar. */
    public Mensaje etiqueta() {
        if (total >= 85) return Mensaje.de("compat.etiqueta.excelente");
        if (total >= 70) return Mensaje.de("compat.etiqueta.muy");
        if (total >= 50) return Mensaje.de("compat.etiqueta.buena");
        if (total >= 30) return Mensaje.de("compat.etiqueta.parcial");
        return Mensaje.de("compat.etiqueta.poca");
    }

    /** El factor que mas ha aportado. Util para titulares. */
    public FactorCompatibilidad factorDominante() {
        return factores.stream()
                .filter(FactorCompatibilidad::aplicable)
                .max(Comparator.comparingDouble(FactorCompatibilidad::puntos))
                .orElse(factores.isEmpty() ? null : factores.get(0));
    }

    /**
     * Factores que no se han podido evaluar por falta de datos.
     *
     * <p>La interfaz los usa para avisar de que la sugerencia esta hecha con
     * informacion incompleta, en vez de presentar la puntuacion como si lo
     * supieramos todo.
     */
    public List<String> factoresSinDatos() {
        return factores.stream()
                .filter(f -> !f.aplicable())
                .map(FactorCompatibilidad::nombre)
                .toList();
    }

    /** Si la puntuacion se ha calculado con todos los factores disponibles. */
    public boolean esCompleta() {
        return factoresSinDatos().isEmpty();
    }
}
