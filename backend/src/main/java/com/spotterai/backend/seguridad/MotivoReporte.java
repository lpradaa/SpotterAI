package com.spotterai.backend.seguridad;

import java.util.Locale;
import java.util.Optional;

/**
 * Por que se reporta a alguien.
 *
 * <p>Una lista cerrada y no un campo de texto libre a secas, porque sin
 * categorias un reporte se lee de uno en uno para saber de que trata, y con
 * ellas se puede saber de un vistazo si hay un patron —diez reportes de acoso
 * contra la misma persona pesan distinto que diez de spam repartidos entre
 * cuarenta—.
 *
 * <p>{@link #OTRO} existe para no obligar a mentir con la categoria mas
 * parecida cuando ninguna encaja; el detalle libre que acompaña al reporte es
 * donde se cuenta el resto.
 */
public enum MotivoReporte {

    COMPORTAMIENTO_INAPROPIADO("Comportamiento inapropiado"),
    PERFIL_FALSO("Perfil falso o suplantación"),
    ACOSO("Acoso o mensajes no deseados"),
    SPAM("Spam o publicidad"),
    OTRO("Otro motivo");

    private final String nombre;

    MotivoReporte(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public static Optional<MotivoReporte> desde(String valor) {
        if (valor == null || valor.isBlank()) return Optional.empty();
        return java.util.Arrays.stream(values())
                .filter(m -> m.name().equalsIgnoreCase(valor.trim().toUpperCase(Locale.ROOT)))
                .findFirst();
    }
}
