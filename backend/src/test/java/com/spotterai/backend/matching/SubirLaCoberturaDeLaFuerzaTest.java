package com.spotterai.backend.matching;

import com.spotterai.backend.models.Levantamiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dos formas de arreglar el factor que peor rinde, medidas antes de elegir.
 *
 * <h2>El problema</h2>
 *
 * <p>La fuerza vale 10 puntos y solo se puede evaluar en una de cada cuatro
 * parejas: exige que los dos hayan apuntado marcas <b>del mismo ejercicio</b>,
 * y con seis para elegir eso pasa poco. Sale medido en
 * {@code SensibilidadDelMotorTest} y esta contado en {@code docs/medir-el-motor.md}.
 *
 * <h2>Las dos salidas</h2>
 *
 * <ul>
 *   <li><b>A. Sugerir los basicos.</b> Cambio de producto: el formulario propone
 *       sentadilla, banca y peso muerto en vez de ofrecer seis a secas. No toca
 *       el motor.</li>
 *   <li><b>B. Comparar equivalentes.</b> Cambio del motor: si uno apunto press
 *       de banca y el otro press militar, compararlos igualmente porque son el
 *       mismo patron.</li>
 * </ul>
 *
 * <h2>Por que no se implementa ninguna todavia</h2>
 *
 * <p>Porque la B tiene un coste que no se ve hasta que se mide: <b>un peso
 * muerto y una sentadilla no son el mismo numero</b>. La gente levanta bastante
 * mas en peso muerto, asi que comparar sus maximos estimados da un ratio bajo
 * aunque las dos personas sean igual de fuertes — y el motor lo leeria como "no
 * podeis cubriros", que es una afirmacion, no un "no se sabe". Subir la
 * cobertura ensuciando la señal no es arreglar el factor: es hacerlo mentir mas
 * a menudo.
 *
 * <p>Lo que se hace aqui es medir las dos: cuanta cobertura gana cada una y,
 * en el caso de la B, cuantas veces cambiaria el veredicto respecto a la
 * comparacion honesta. Con eso ya se puede elegir.
 */
class SubirLaCoberturaDeLaFuerzaTest {

    private static final int CUANTOS = 60;

    /**
     * Los patrones de movimiento, tal y como los agruparia la opcion B.
     *
     * <p>Es la agrupacion que tendria sentido en el gimnasio: empujar por
     * encima de la cabeza y empujar tumbado son los dos empuje; sentadilla y
     * peso muerto son las dos tren inferior con barra.
     */
    private static final Map<Ejercicio, String> PATRON = new EnumMap<>(Map.of(
            Ejercicio.PRESS_BANCA, "empuje",
            Ejercicio.PRESS_MILITAR, "empuje",
            Ejercicio.SENTADILLA, "pierna",
            Ejercicio.PESO_MUERTO, "pierna",
            Ejercicio.HIP_THRUST, "pierna",
            Ejercicio.REMO_BARRA, "tiron"));

    private static List<PerfilDeMatch> poblacion(BancoDePerfiles.RepartoDeEjercicios reparto) {
        return BancoDePerfiles.poblacion(CUANTOS, true, reparto);
    }

    /** Cuantas parejas tienen al menos un ejercicio en comun, comparando exacto. */
    private static int parejasConDatos(List<PerfilDeMatch> gente) {
        int conDatos = 0;
        for (int i = 0; i < gente.size(); i++) {
            for (int j = i + 1; j < gente.size(); j++) {
                if (CalculadoraFuerza.comparar(gente.get(i).levantamientos(),
                        gente.get(j).levantamientos()).hayDatos()) {
                    conDatos++;
                }
            }
        }
        return conDatos;
    }

    /** Lo mismo, pero agrupando por patron: es la opcion B simulada. */
    private static int parejasConDatosPorPatron(List<PerfilDeMatch> gente) {
        int conDatos = 0;
        for (int i = 0; i < gente.size(); i++) {
            for (int j = i + 1; j < gente.size(); j++) {
                if (!patronesEnComun(gente.get(i).levantamientos(),
                        gente.get(j).levantamientos()).isEmpty()) {
                    conDatos++;
                }
            }
        }
        return conDatos;
    }

    private static List<String> patronesEnComun(List<Levantamiento> mios, List<Levantamiento> suyos) {
        List<String> comunes = new ArrayList<>();
        for (Levantamiento mio : mios) {
            for (Levantamiento suyo : suyos) {
                String patron = PATRON.get(mio.getEjercicio());
                if (patron.equals(PATRON.get(suyo.getEjercicio())) && !comunes.contains(patron)) {
                    comunes.add(patron);
                }
            }
        }
        return comunes;
    }

    private static int totalDeParejas() {
        return CUANTOS * (CUANTOS - 1) / 2;
    }

    @Test
    @DisplayName("Informe: cuanto gana cada salida, y cuanto cuesta")
    void informe() {
        List<PerfilDeMatch> comoHoy = poblacion(BancoDePerfiles.RepartoDeEjercicios.COMO_HOY);
        List<PerfilDeMatch> conBasicos =
                poblacion(BancoDePerfiles.RepartoDeEjercicios.SUGIRIENDO_LOS_BASICOS);

        int total = totalDeParejas();
        int hoy = parejasConDatos(comoHoy);
        int conA = parejasConDatos(conBasicos);
        int conB = parejasConDatosPorPatron(comoHoy);

        System.out.println();
        System.out.println("=== Subir la cobertura de la fuerza ===");
        System.out.printf("Poblacion: %d perfiles, %d parejas (semilla %d)%n",
                CUANTOS, total, BancoDePerfiles.SEMILLA);
        System.out.println();
        System.out.printf("  hoy                        %4d parejas con datos  (%.1f %%)%n",
                hoy, 100.0 * hoy / total);
        System.out.printf("  A. sugiriendo los basicos  %4d                     (%.1f %%)%n",
                conA, 100.0 * conA / total);
        System.out.printf("  B. comparando por patron   %4d                     (%.1f %%)%n",
                conB, 100.0 * conB / total);
        System.out.println();

        // El coste de la B: cuantas veces diria algo distinto de la verdad.
        Ruido ruido = medirElRuidoDeLaB(comoHoy);
        System.out.println("  Coste de la B, medido solo donde se sabe la verdad");
        System.out.println("  (parejas que comparten ejercicio exacto Y ademas patron):");
        System.out.printf("    comparables            %4d%n", ruido.comparables);
        System.out.printf("    veredicto distinto     %4d  (%.1f %%)%n",
                ruido.cambian, 100.0 * ruido.cambian / Math.max(1, ruido.comparables));
        System.out.println();

        assertTrue(total > 0);
    }

    private record Ruido(int comparables, int cambian) {}

    /**
     * Cuanto mentiria la opcion B, medido donde se puede saber.
     *
     * <p>En las parejas que comparten un ejercicio exacto sabemos el veredicto
     * honesto: "podeis cubriros" o no. En esas mismas parejas se calcula lo que
     * diria la version por patron, que compara maximos de ejercicios distintos.
     * La diferencia entre los dos es el ruido que introduce.
     */
    private static Ruido medirElRuidoDeLaB(List<PerfilDeMatch> gente) {
        int comparables = 0;
        int cambian = 0;

        for (int i = 0; i < gente.size(); i++) {
            for (int j = i + 1; j < gente.size(); j++) {
                List<Levantamiento> mios = gente.get(i).levantamientos();
                List<Levantamiento> suyos = gente.get(j).levantamientos();

                CalculadoraFuerza.Comparacion honesta = CalculadoraFuerza.comparar(mios, suyos);
                if (!honesta.hayDatos()) continue;

                Boolean verdad = honesta.podeisCubriros().orElse(null);
                Boolean porPatron = veredictoPorPatron(mios, suyos);
                if (verdad == null || porPatron == null) continue;

                comparables++;
                if (!verdad.equals(porPatron)) cambian++;
            }
        }
        return new Ruido(comparables, cambian);
    }

    /**
     * Lo que diria la opcion B: el mismo calculo, pero emparejando por patron.
     *
     * <p>Con el mismo umbral que usa el motor de verdad, para que la unica
     * diferencia sea que ejercicios se comparan entre si.
     */
    private static Boolean veredictoPorPatron(List<Levantamiento> mios, List<Levantamiento> suyos) {
        Map<String, Double> porMio = maximosPorPatron(mios);
        Map<String, Double> porSuyo = maximosPorPatron(suyos);

        double suma = 0;
        int cuantos = 0;

        for (Map.Entry<String, Double> entrada : porMio.entrySet()) {
            Double suyo = porSuyo.get(entrada.getKey());
            if (suyo == null) continue;

            double mio = entrada.getValue();
            suma += Math.min(mio, suyo) / Math.max(mio, suyo);
            cuantos++;
        }

        if (cuantos == 0) return null;
        // 0,70 es el ratio con el que el motor considera que uno puede asistir
        // al otro con comodidad.
        return suma / cuantos >= 0.70;
    }

    private static Map<String, Double> maximosPorPatron(List<Levantamiento> marcas) {
        Map<String, Double> maximos = new java.util.HashMap<>();
        for (Levantamiento l : marcas) {
            double estimado = CalculadoraFuerza.maximoEstimado(l.getPeso(), l.getRepeticiones());
            maximos.merge(PATRON.get(l.getEjercicio()), estimado, Math::max);
        }
        return maximos;
    }

    /**
     * La opcion A sube la cobertura sin tocar el motor.
     *
     * <p>Es la unica de las dos que no cambia lo que significa el numero: se
     * siguen comparando maximos del mismo ejercicio, que es lo que hace que el
     * ratio quiera decir algo. Lo unico que cambia es que hay mas parejas con
     * ese dato.
     */
    @Test
    @DisplayName("A. Sugerir los basicos sube la cobertura sin tocar el motor")
    void sugerirLosBasicosSubeLaCobertura() {
        int hoy = parejasConDatos(poblacion(BancoDePerfiles.RepartoDeEjercicios.COMO_HOY));
        int conBasicos = parejasConDatos(
                poblacion(BancoDePerfiles.RepartoDeEjercicios.SUGIRIENDO_LOS_BASICOS));

        assertTrue(conBasicos > hoy,
                "Sugerir los basicos deberia subir la cobertura: hoy " + hoy
                        + " parejas con datos, sugiriendolos " + conBasicos);
    }

    /**
     * Y la B tambien la sube — pero eso no basta para elegirla.
     *
     * <p>Esta prueba existe para dejar claro que las dos "funcionan" en la
     * metrica facil. La pregunta que decide es la otra: que le pasa a la señal.
     */
    @Test
    @DisplayName("B. Comparar por patron tambien sube la cobertura")
    void compararPorPatronTambienLaSube() {
        List<PerfilDeMatch> gente = poblacion(BancoDePerfiles.RepartoDeEjercicios.COMO_HOY);

        assertTrue(parejasConDatosPorPatron(gente) > parejasConDatos(gente));
    }

    /**
     * El motivo por el que la B no se implementa.
     *
     * <p>Se mide solo donde se puede saber la verdad —parejas que comparten
     * ejercicio exacto— y se comprueba cuantas veces la version por patron
     * dice lo contrario. Si cambiara el veredicto de una parte apreciable, el
     * factor estaria afirmando "podeis cubriros" o "no podeis" sobre una
     * comparacion entre ejercicios que no son el mismo.
     *
     * <p>El umbral no es un objetivo de calidad: es la linea a partir de la
     * cual el ruido deja de ser un matiz. Si algun dia la B se implementa, este
     * numero es el que hay que poner encima de la mesa.
     */
    @Test
    @DisplayName("Pero la B cambia veredictos: sube la cobertura ensuciando la señal")
    void laBEnsuciaLaSenal() {
        Ruido ruido = medirElRuidoDeLaB(poblacion(BancoDePerfiles.RepartoDeEjercicios.COMO_HOY));

        assertTrue(ruido.comparables() > 0,
                "Sin parejas comparables no se puede decir nada del ruido");

        double porcentaje = 100.0 * ruido.cambian() / ruido.comparables();
        assertTrue(porcentaje > 0,
                "Si comparar por patron no cambiara ningun veredicto, no habria coste "
                        + "y la opcion B seria gratis: conviene volver a mirarlo");
    }
}
