package com.spotterai.backend.matching;

import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Usuario;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Motor de compatibilidad de SpotterAI.
 *
 * <p>Puntua de 0 a 100 lo bien que encajan dos personas como companeros de
 * entrenamiento. El calculo es <strong>deterministico</strong> a proposito: dos
 * usuarios con los mismos datos dan siempre el mismo numero, se puede testear, y
 * cuesta cero. La IA se usa despues, para redactar el porque; nunca para puntuar.
 *
 * <p>Reparto de pesos:
 * <pre>
 *   Horario    40  - decisivo: sin solape no hay entrenamiento posible
 *   Nivel      20  - marca el ritmo y las cargas
 *   Objetivo   20  - determina el tipo de sesion
 *   Gimnasio   15  - condicion practica
 *   Edad        5  - afinidad menor
 * </pre>
 */
public final class CalculadoraCompatibilidad {

    private static final double PESO_HORARIO = 40;
    private static final double PESO_NIVEL = 20;
    private static final double PESO_OBJETIVO = 20;
    private static final double PESO_GIMNASIO = 15;
    private static final double PESO_EDAD = 5;

    /** A partir de estos minutos semanales en comun se considera solape de sobra. */
    private static final double MINUTOS_SOLAPE_IDEAL = 300; // 5 h/semana

    /** A partir de estos dias en comun se considera flexibilidad de sobra. */
    private static final double DIAS_SOLAPE_IDEAL = 3;

    private static final Map<String, Integer> ESCALA_NIVEL =
            Map.of("principiante", 1, "intermedio", 2, "avanzado", 3);

    /**
     * Objetivos que, sin ser iguales, comparten tipo de entrenamiento: quien busca
     * fuerza y quien busca hipertrofia pueden compartir sesion sin problema.
     */
    private static final List<Set<String>> OBJETIVOS_AFINES = List.of(
            Set.of("hipertrofia", "fuerza"),
            Set.of("perdida de peso", "resistencia")
    );

    private CalculadoraCompatibilidad() {}

    public static PuntuacionCompatibilidad calcular(
            Usuario yo, List<Disponibilidad> misHorarios,
            Usuario otro, List<Disponibilidad> susHorarios) {

        SolapeHorario solape = CalculadoraSolape.calcular(misHorarios, susHorarios);

        List<FactorCompatibilidad> factores = new ArrayList<>();
        factores.add(factorHorario(solape));
        factores.add(factorNivel(yo.getNivel(), otro.getNivel()));
        factores.add(factorObjetivo(yo.getObjetivos(), otro.getObjetivos()));
        factores.add(factorGimnasio(yo, otro));
        factores.add(factorEdad(yo.getEdad(), otro.getEdad()));

        double suma = factores.stream().mapToDouble(FactorCompatibilidad::puntos).sum();
        return new PuntuacionCompatibilidad((int) Math.round(suma), List.copyOf(factores), solape);
    }

    private static FactorCompatibilidad factorHorario(SolapeHorario solape) {
        if (!solape.hayCoincidencia()) {
            return new FactorCompatibilidad("horario", 0, PESO_HORARIO,
                    "Vuestros horarios no coinciden en ningun momento de la semana");
        }

        // Dos componentes: cuanto tiempo coincidis y en cuantos dias distintos.
        // Los dias importan porque tres franjas cortas repartidas dan mas margen
        // para quedar que una sola franja larga.
        double porTiempo = Math.min(1.0, solape.minutosSemanales() / MINUTOS_SOLAPE_IDEAL);
        double porDias = Math.min(1.0, solape.dias().size() / DIAS_SOLAPE_IDEAL);
        double ratio = porTiempo * 0.7 + porDias * 0.3;

        String detalle = "Coincidis %s a la semana en %s".formatted(
                formatearDuracion(solape.minutosSemanales()),
                enumerar(solape.dias()));

        return new FactorCompatibilidad("horario", ratio * PESO_HORARIO, PESO_HORARIO, detalle);
    }

    private static FactorCompatibilidad factorNivel(String miNivel, String suNivel) {
        // Map.of() lanza NullPointerException al consultar una clave nula, asi que
        // un perfil sin nivel se descarta antes de tocar el mapa.
        String claveA = normalizar(miNivel);
        String claveB = normalizar(suNivel);
        Integer a = claveA == null ? null : ESCALA_NIVEL.get(claveA);
        Integer b = claveB == null ? null : ESCALA_NIVEL.get(claveB);

        if (a == null || b == null) {
            return new FactorCompatibilidad("nivel", 0, PESO_NIVEL,
                    "Falta el nivel de alguno de los dos perfiles");
        }

        int distancia = Math.abs(a - b);
        return switch (distancia) {
            case 0 -> new FactorCompatibilidad("nivel", PESO_NIVEL, PESO_NIVEL,
                    "Los dos entrenais a nivel " + suNivel.toLowerCase(Locale.ROOT));
            case 1 -> new FactorCompatibilidad("nivel", PESO_NIVEL / 2, PESO_NIVEL,
                    "Estais en niveles contiguos (%s y %s), asumible con algun ajuste de cargas"
                            .formatted(miNivel, suNivel));
            default -> new FactorCompatibilidad("nivel", 0, PESO_NIVEL,
                    "Hay dos escalones de diferencia entre %s y %s".formatted(miNivel, suNivel));
        };
    }

    private static FactorCompatibilidad factorObjetivo(String miObjetivo, String suObjetivo) {
        String a = normalizar(miObjetivo);
        String b = normalizar(suObjetivo);

        if (a == null || b == null) {
            return new FactorCompatibilidad("objetivo", 0, PESO_OBJETIVO,
                    "Falta el objetivo de alguno de los dos perfiles");
        }
        if (a.equals(b)) {
            return new FactorCompatibilidad("objetivo", PESO_OBJETIVO, PESO_OBJETIVO,
                    "Buscais lo mismo: " + suObjetivo.toLowerCase(Locale.ROOT));
        }
        boolean afines = OBJETIVOS_AFINES.stream().anyMatch(g -> g.contains(a) && g.contains(b));
        if (afines) {
            return new FactorCompatibilidad("objetivo", PESO_OBJETIVO / 2, PESO_OBJETIVO,
                    "Objetivos distintos pero compatibles: %s y %s".formatted(miObjetivo, suObjetivo));
        }
        return new FactorCompatibilidad("objetivo", 0, PESO_OBJETIVO,
                "Entrenais para cosas distintas: %s frente a %s".formatted(miObjetivo, suObjetivo));
    }

    private static FactorCompatibilidad factorGimnasio(Usuario yo, Usuario otro) {
        Long mio = yo.getGimnasio() != null ? yo.getGimnasio().getId() : null;
        Long suyo = otro.getGimnasio() != null ? otro.getGimnasio().getId() : null;

        if (mio == null || suyo == null) {
            return new FactorCompatibilidad("gimnasio", 0, PESO_GIMNASIO,
                    "Falta el gimnasio de alguno de los dos perfiles");
        }
        if (mio.equals(suyo)) {
            String nombre = otro.getGimnasio().getNombre();
            return new FactorCompatibilidad("gimnasio", PESO_GIMNASIO, PESO_GIMNASIO,
                    "Entrenais en el mismo gimnasio: " + nombre);
        }
        return new FactorCompatibilidad("gimnasio", 0, PESO_GIMNASIO, "Entrenais en gimnasios distintos");
    }

    private static FactorCompatibilidad factorEdad(Integer miEdad, Integer suEdad) {
        if (miEdad == null || suEdad == null) {
            return new FactorCompatibilidad("edad", 0, PESO_EDAD,
                    "Falta la edad de alguno de los dos perfiles");
        }

        int diferencia = Math.abs(miEdad - suEdad);
        double puntos;
        if (diferencia <= 3) puntos = PESO_EDAD;
        else if (diferencia <= 7) puntos = PESO_EDAD * 0.6;
        else if (diferencia <= 12) puntos = PESO_EDAD * 0.2;
        else puntos = 0;

        String detalle = diferencia <= 3
                ? "Teneis practicamente la misma edad"
                : "Os llevais %d anos".formatted(diferencia);
        return new FactorCompatibilidad("edad", puntos, PESO_EDAD, detalle);
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }

    static String formatearDuracion(int minutos) {
        int horas = minutos / 60;
        int resto = minutos % 60;
        if (horas == 0) return resto + " min";
        if (resto == 0) return horas == 1 ? "1 hora" : horas + " horas";
        return "%dh %dmin".formatted(horas, resto);
    }

    static String enumerar(List<String> elementos) {
        if (elementos.isEmpty()) return "";
        if (elementos.size() == 1) return elementos.get(0);
        String ultimo = elementos.get(elementos.size() - 1);
        String previos = String.join(", ", elementos.subList(0, elementos.size() - 1));
        return previos + " y " + ultimo;
    }
}
