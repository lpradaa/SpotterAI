package com.spotterai.backend.matching;

import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Levantamiento;
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
 * <p>Reparto de pesos de referencia:
 * <pre>
 *   Horario    40  decisivo: sin solape no hay entrenamiento posible
 *   Objetivo   20  determina el tipo de sesion
 *   Gimnasio   15  condicion practica
 *   Nivel      10  el ritmo, tal y como lo declara cada uno
 *   Fuerza     10  si podeis cubriros con la barra cargada
 *   Edad        5  afinidad menor
 * </pre>
 *
 * <p>Nivel y fuerza miden lo mismo por dos caminos: uno es una etiqueta elegida
 * y el otro un numero comprobable. De ahi que se repartan los 20 que antes se
 * llevaba el nivel solo.
 *
 * <p>Son pesos <em>de referencia</em>, no fijos: un factor sin datos en alguno de
 * los dos perfiles sale del calculo y reparte su peso proporcionalmente entre los
 * demas. Asi, no rellenar un campo deja de puntuar como un mal encaje y el total
 * sigue estando sobre 100.
 */
public final class CalculadoraCompatibilidad {

    // Visibles fuera para que el aviso de "esto te esta costando puntos" use los
    // mismos numeros que el calculo. Duplicarlos en el frontend seria garantizar
    // que un dia digan cosas distintas.
    static final double PESO_HORARIO = 40;
    /*
     * Nivel baja de 20 a 10 y esos 10 pasan a fuerza.
     *
     * Los dos miden lo mismo, pero uno es una etiqueta que se elige y el otro un
     * hecho: "Intermedio" significa cosas distintas para cada persona, y 100 kg
     * en banca no. Se le deja la mitad porque sigue sirviendo para quien no ha
     * registrado ningun levantamiento, que sera la mayoria al principio.
     */
    static final double PESO_NIVEL = 10;
    static final double PESO_FUERZA = 10;
    static final double PESO_OBJETIVO = 20;
    static final double PESO_GIMNASIO = 15;
    static final double PESO_EDAD = 5;

    /** Minutos efectivos semanales a partir de los cuales el solape es de sobra. */
    private static final double MINUTOS_SOLAPE_IDEAL = 300; // 5 h/semana

    /** Dias distintos con solape a partir de los cuales hay flexibilidad de sobra. */
    private static final double DIAS_SOLAPE_IDEAL = 3;

    /** Reparto interno del factor horario entre volumen y variedad de dias. */
    private static final double SUBPESO_TIEMPO = 0.70;
    private static final double SUBPESO_DIAS = 0.30;

    /**
     * Suelo que garantiza el compromiso mutuo.
     *
     * <p>El ancla no compite con los minutos: los <em>ignora</em>. Coincidir una hora
     * a la que los dos vais siempre predice mejor que entreneis juntos que seis horas
     * de disponibilidad vaga compartida, asi que en vez de sumar puntos por duracion,
     * garantiza un minimo del factor.
     *
     * <p>Antes esto era un sumando con la mitad del peso, y tenia un efecto que solo
     * se vio midiendo: todo usuario que no hubiera marcado franjas fijas —es decir,
     * todos los existentes— tenia la mitad del factor horario bloqueada de partida.
     */
    private static final double SUELO_UNA_ANCLA = 0.75;
    private static final double SUELO_VARIAS_ANCLAS = 0.95;

    /**
     * Cuanto se descuenta a una puntuacion calculada con datos incompletos.
     *
     * <p>Repartir el peso de los factores ausentes evita penalizar a quien no ha
     * rellenado el perfil, pero por si solo produce algo peor: un perfil vacio con
     * dos factores coincidentes sacaba 100 y adelantaba a otro con cinco factores y
     * solape horario real. Menos informacion no puede dar mejor nota.
     *
     * <p>Con esto, el techo sube con la evidencia disponible: sin ningun dato se
     * queda en el 30 % y solo un perfil completo puede llegar a 100.
     */
    private static final double CONFIANZA_BASE = 0.30;

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

    /** Sin levantamientos, para quien llame sin ese dato. */
    public static PuntuacionCompatibilidad calcular(
            Usuario yo, List<Disponibilidad> misHorarios,
            Usuario otro, List<Disponibilidad> susHorarios) {
        return calcular(yo, misHorarios, List.of(), otro, susHorarios, List.of());
    }

    public static PuntuacionCompatibilidad calcular(
            Usuario yo, List<Disponibilidad> misHorarios, List<Levantamiento> misLevantamientos,
            Usuario otro, List<Disponibilidad> susHorarios, List<Levantamiento> susLevantamientos) {

        SolapeHorario solape = CalculadoraSolape.calcular(misHorarios, susHorarios);

        List<FactorCompatibilidad> brutos = List.of(
                factorHorario(solape, misHorarios, susHorarios),
                factorNivel(yo.getNivel(), otro.getNivel()),
                factorFuerza(misLevantamientos, susLevantamientos),
                factorObjetivo(yo.getObjetivos(), otro.getObjetivos()),
                factorGimnasio(yo, otro),
                factorEdad(yo.getEdad(), otro.getEdad()));

        List<FactorCompatibilidad> factores = repartirPesoDeLosNoAplicables(brutos);

        double suma = factores.stream().mapToDouble(FactorCompatibilidad::puntos).sum();
        double total = suma * confianzaPorEvidencia(brutos);

        return new PuntuacionCompatibilidad((int) Math.round(total), factores, solape);
    }

    /**
     * Cuanta confianza merece la puntuacion segun la evidencia disponible.
     *
     * <p>Va de {@value #CONFIANZA_BASE} sin ningun dato a 1 con el perfil completo.
     * Sin esto, repartir el peso de los factores ausentes hace que un perfil a medias
     * saque mejor nota que uno completo: es lo que aparecio al medir contra datos
     * reales, con usuarios sin horarios encabezando la lista por delante de otros con
     * seis horas de solape.
     */
    private static double confianzaPorEvidencia(List<FactorCompatibilidad> brutos) {
        double pesoTotal = PESO_HORARIO + PESO_NIVEL + PESO_FUERZA
                + PESO_OBJETIVO + PESO_GIMNASIO + PESO_EDAD;
        double pesoEvaluado = brutos.stream()
                .filter(FactorCompatibilidad::aplicable)
                .mapToDouble(FactorCompatibilidad::puntosMax)
                .sum();

        return CONFIANZA_BASE + (1 - CONFIANZA_BASE) * (pesoEvaluado / pesoTotal);
    }

    /**
     * Reescala los factores aplicables para que sigan sumando 100 cuando alguno se
     * ha quedado fuera por falta de datos.
     *
     * <p>Ejemplo: si no sabemos los horarios de ninguno de los dos, sus 40 puntos se
     * reparten proporcionalmente entre nivel, objetivo, gimnasio y edad, que pasan a
     * valer 100 entre los cuatro. El orden de los candidatos lo deciden entonces los
     * datos que si tenemos, en vez de hundir a todo el mundo por igual.
     */
    private static List<FactorCompatibilidad> repartirPesoDeLosNoAplicables(
            List<FactorCompatibilidad> brutos) {

        double pesoDisponible = brutos.stream()
                .filter(FactorCompatibilidad::aplicable)
                .mapToDouble(FactorCompatibilidad::puntosMax)
                .sum();

        // Sin ningun factor evaluable no hay nada que repartir; se devuelven tal cual
        // y la puntuacion sera 0, que en ese caso es la respuesta honesta.
        if (pesoDisponible == 0) return List.copyOf(brutos);

        double escala = 100.0 / pesoDisponible;

        List<FactorCompatibilidad> ajustados = new ArrayList<>(brutos.size());
        for (FactorCompatibilidad f : brutos) {
            ajustados.add(f.aplicable() ? f.conPeso(f.puntosMax() * escala) : f);
        }
        return List.copyOf(ajustados);
    }

    private static FactorCompatibilidad factorHorario(
            SolapeHorario solape, List<Disponibilidad> mios, List<Disponibilidad> suyos) {

        // Sin horarios en alguno de los dos perfiles no es que no coincidais: es que
        // no lo sabemos. El factor sale del calculo en vez de restar 40 puntos.
        boolean faltanDatos = mios == null || mios.isEmpty() || suyos == null || suyos.isEmpty();
        if (faltanDatos) {
            return FactorCompatibilidad.sinDatos("horario",
                    "Faltan los horarios de alguno de los dos perfiles");
        }

        if (!solape.hayCoincidencia()) {
            return FactorCompatibilidad.evaluado("horario", 0, PESO_HORARIO,
                    "Vuestros horarios no coinciden en ningún momento de la semana");
        }

        double ratioTiempo = Math.min(1.0, solape.minutosEfectivos() / MINUTOS_SOLAPE_IDEAL);
        double ratioDias = Math.min(1.0, solape.dias().size() / DIAS_SOLAPE_IDEAL);

        // El volumen nunca puede superar la fiabilidad del propio solape: declarar
        // disponibilidad de sobra sube la puntuacion, pero con un techo proporcional
        // a lo que te comprometes. Sin esto, la mejor estrategia seria decir que
        // puedes a todas horas.
        double volumen = Math.min(
                solape.fiabilidad(),
                SUBPESO_TIEMPO * ratioTiempo + SUBPESO_DIAS * ratioDias);

        double ratio = Math.max(volumen, sueloPorAnclas(solape.diasAncla()));

        return FactorCompatibilidad.evaluado("horario", ratio * PESO_HORARIO, PESO_HORARIO,
                describirSolape(solape));
    }

    private static double sueloPorAnclas(int diasAncla) {
        if (diasAncla >= 2) return SUELO_VARIAS_ANCLAS;
        if (diasAncla == 1) return SUELO_UNA_ANCLA;
        return 0;
    }

    private static String describirSolape(SolapeHorario solape) {
        if (solape.hayAncla()) {
            String dias = solape.diasAncla() == 1 ? "un día" : solape.diasAncla() + " días";
            return "Los dos vais siempre %s a la misma hora (%s)".formatted(
                    dias, enumerar(solape.dias()));
        }
        return "Coincidís %s a la semana en %s".formatted(
                formatearDuracion(solape.minutosSemanales()), enumerar(solape.dias()));
    }

    /**
     * Si podeis cubriros el uno al otro con la barra cargada.
     *
     * El detalle de por que se compara asi esta en {@link CalculadoraFuerza}.
     */
    private static FactorCompatibilidad factorFuerza(
            List<Levantamiento> mios, List<Levantamiento> suyos) {

        CalculadoraFuerza.Comparacion comparacion = CalculadoraFuerza.comparar(mios, suyos);

        if (!comparacion.hayDatos()) {
            // Sin ejercicios en comun no es que seais incompatibles: es que no
            // hay nada que comparar, y eso no debe restar.
            return FactorCompatibilidad.sinDatos("fuerza",
                    "No hay levantamientos en común que comparar");
        }

        return FactorCompatibilidad.evaluado("fuerza",
                comparacion.ratio() * PESO_FUERZA, PESO_FUERZA,
                CalculadoraFuerza.describir(comparacion));
    }

    private static FactorCompatibilidad factorNivel(String miNivel, String suNivel) {
        // Map.of() lanza NullPointerException al consultar una clave nula, asi que
        // un perfil sin nivel se descarta antes de tocar el mapa.
        String claveA = normalizar(miNivel);
        String claveB = normalizar(suNivel);
        Integer a = claveA == null ? null : ESCALA_NIVEL.get(claveA);
        Integer b = claveB == null ? null : ESCALA_NIVEL.get(claveB);

        if (a == null || b == null) {
            return FactorCompatibilidad.sinDatos("nivel",
                    "Falta el nivel de alguno de los dos perfiles");
        }

        int distancia = Math.abs(a - b);
        return switch (distancia) {
            case 0 -> FactorCompatibilidad.evaluado("nivel", PESO_NIVEL, PESO_NIVEL,
                    "Los dos entrenáis a nivel " + suNivel.toLowerCase(Locale.ROOT));
            case 1 -> FactorCompatibilidad.evaluado("nivel", PESO_NIVEL / 2, PESO_NIVEL,
                    "Estáis en niveles contiguos (%s y %s), asumible ajustando las cargas"
                            .formatted(miNivel, suNivel));
            default -> FactorCompatibilidad.evaluado("nivel", 0, PESO_NIVEL,
                    "Hay dos escalones de diferencia entre %s y %s".formatted(miNivel, suNivel));
        };
    }

    private static FactorCompatibilidad factorObjetivo(String miObjetivo, String suObjetivo) {
        String a = normalizar(miObjetivo);
        String b = normalizar(suObjetivo);

        if (a == null || b == null) {
            return FactorCompatibilidad.sinDatos("objetivo",
                    "Falta el objetivo de alguno de los dos perfiles");
        }
        if (a.equals(b)) {
            return FactorCompatibilidad.evaluado("objetivo", PESO_OBJETIVO, PESO_OBJETIVO,
                    "Buscáis lo mismo: " + suObjetivo.toLowerCase(Locale.ROOT));
        }
        boolean afines = OBJETIVOS_AFINES.stream().anyMatch(g -> g.contains(a) && g.contains(b));
        if (afines) {
            return FactorCompatibilidad.evaluado("objetivo", PESO_OBJETIVO / 2, PESO_OBJETIVO,
                    "Objetivos distintos pero compatibles: %s y %s".formatted(miObjetivo, suObjetivo));
        }
        return FactorCompatibilidad.evaluado("objetivo", 0, PESO_OBJETIVO,
                "Entrenáis para cosas distintas: %s frente a %s".formatted(miObjetivo, suObjetivo));
    }

    private static FactorCompatibilidad factorGimnasio(Usuario yo, Usuario otro) {
        Long mio = yo.getGimnasio() != null ? yo.getGimnasio().getId() : null;
        Long suyo = otro.getGimnasio() != null ? otro.getGimnasio().getId() : null;

        if (mio == null || suyo == null) {
            return FactorCompatibilidad.sinDatos("gimnasio",
                    "Falta el gimnasio de alguno de los dos perfiles");
        }
        if (mio.equals(suyo)) {
            return FactorCompatibilidad.evaluado("gimnasio", PESO_GIMNASIO, PESO_GIMNASIO,
                    "Entrenáis en el mismo gimnasio: " + otro.getGimnasio().getNombre());
        }
        return FactorCompatibilidad.evaluado("gimnasio", 0, PESO_GIMNASIO,
                "Entrenáis en gimnasios distintos");
    }

    private static FactorCompatibilidad factorEdad(Integer miEdad, Integer suEdad) {
        if (miEdad == null || suEdad == null) {
            return FactorCompatibilidad.sinDatos("edad",
                    "Falta la edad de alguno de los dos perfiles");
        }

        int diferencia = Math.abs(miEdad - suEdad);
        double puntos;
        if (diferencia <= 3) puntos = PESO_EDAD;
        else if (diferencia <= 7) puntos = PESO_EDAD * 0.6;
        else if (diferencia <= 12) puntos = PESO_EDAD * 0.2;
        else puntos = 0;

        String detalle = diferencia <= 3
                ? "Tenéis prácticamente la misma edad"
                : "Os lleváis %d años".formatted(diferencia);
        return FactorCompatibilidad.evaluado("edad", puntos, PESO_EDAD, detalle);
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
