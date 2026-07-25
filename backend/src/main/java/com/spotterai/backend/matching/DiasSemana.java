package com.spotterai.backend.matching;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Nombres de los dias de la semana, normalizados.
 *
 * <p>Existe porque el mismo dia llegaba escrito de varias formas segun por donde
 * entrara ("Miércoles", "miercoles", "MIERCOLES"). Eso rompia dos cosas: el
 * cruce de horarios, que no los reconocia como el mismo dia, y el desplegable
 * del perfil, que se quedaba en blanco cuando el valor guardado no coincidia
 * exactamente con una de sus opciones y al guardar escribia un dia vacio.
 *
 * <p>La regla es: se compara sin tildes y en minusculas, se guarda y se muestra
 * en la forma canonica con tilde.
 */
public final class DiasSemana {

    /** Orden natural de la semana, en la forma normalizada que se usa como clave. */
    static final List<String> ORDEN = List.of(
            "lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo");

    /** Forma canonica: como se guarda en base de datos y como se le muestra al usuario. */
    private static final Map<String, String> CANONICO = Map.of(
            "lunes", "Lunes",
            "martes", "Martes",
            "miercoles", "Miércoles",
            "jueves", "Jueves",
            "viernes", "Viernes",
            "sabado", "Sábado",
            "domingo", "Domingo");

    private DiasSemana() {}

    /**
     * Reduce el dia a su clave de comparacion: minusculas y sin tildes.
     *
     * @return la clave, o {@code null} si el valor no es un dia reconocible
     */
    public static String clave(String dia) {
        if (dia == null || dia.isBlank()) return null;
        String limpio = Normalizer.normalize(dia.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);
        return ORDEN.contains(limpio) ? limpio : null;
    }

    /**
     * Devuelve el dia en su forma canonica, lista para guardar o mostrar.
     *
     * @return "Miércoles" para cualquier variante de miercoles; el valor original
     *         sin tocar si no se reconoce, para no perder informacion del usuario
     */
    public static String canonico(String dia) {
        String clave = clave(dia);
        return clave == null ? dia : CANONICO.get(clave);
    }

    /** Forma canonica a partir de una clave ya normalizada. */
    static String desdeClave(String clave) {
        return CANONICO.getOrDefault(clave, clave);
    }

    /** Posicion en la semana, para ordenar. Los desconocidos van al final. */
    static int posicion(String clave) {
        int i = ORDEN.indexOf(clave);
        return i < 0 ? Integer.MAX_VALUE : i;
    }
}
