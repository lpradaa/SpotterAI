package com.spotterai.backend.matching;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cuanto mueve cada peso el numero que se le enseña a la gente.
 *
 * <h2>Que pregunta responde</h2>
 *
 * <p>Los nueve pesos del motor estan razonados uno a uno en
 * {@link CalculadoraCompatibilidad} —por que el horario vale 40 y la edad 5—
 * pero razonar no es medir. Lo que aqui se comprueba es otra cosa: que el
 * reparto <b>hace lo que dice hacer</b> sobre una poblacion variada.
 *
 * <p>La distincion importa porque un peso puede estar perfectamente justificado
 * y no cambiar ninguna decision: si un factor casi nunca tiene datos, o si su
 * valor casi siempre sale igual, sus puntos no separan a nadie de nadie. Eso no
 * se ve leyendo el codigo, solo contando.
 *
 * <h2>Lo que esto NO es</h2>
 *
 * <p>No es una validacion del motor. Validar seria comprobar que la gente con
 * puntuacion alta acaba entrenando junta, y para eso esta el embudo, que hoy
 * dice —correctamente— que no hay muestra. Aqui no hay usuarios: hay una
 * poblacion sintetica y una pregunta interna, "que pasa si toco esto".
 *
 * <h2>Como se mide</h2>
 *
 * <p>Se anula un factor cada vez, poniendo su peso a cero, y se cuenta cuantas
 * parejas cambian de <b>tramo</b>. El tramo y no los puntos: la diferencia entre
 * un 68 y un 71 no la ve nadie, pero la que hay entre "buena compatibilidad" y
 * "muy compatibles" cambia lo que la pantalla dice y probablemente lo que la
 * persona hace. Es la misma metrica con la que se calibro la cuantizacion del
 * modelo — decisiones cambiadas, no distancias.
 */
class SensibilidadDelMotorTest {

    /** 60 perfiles son 1.770 parejas: suficiente para que los porcentajes signifiquen algo. */
    private static final int CUANTOS = 60;

    private static List<PerfilDeMatch> gente;

    /** Los de fabrica, para poder devolverlos a su sitio. */
    private static final PesosDelMotor DE_FABRICA =
            new PesosDelMotor(40, 10, 10, 12, 10, 5, 8, 5);

    @BeforeAll
    static void prepararPoblacion() {
        gente = BancoDePerfiles.poblacion(CUANTOS, true);
    }

    @AfterEach
    void devolverLosPesos() {
        // Son estaticos y compartidos por toda la maquina virtual: dejarlos
        // tocados haria que el resto de la suite midiera otro motor.
        CalculadoraCompatibilidad.configurar(DE_FABRICA);
    }

    /** El tramo con el que se etiqueta a una pareja, que es lo que se lee. */
    private static String tramo(int total) {
        if (total >= 85) return "excelente";
        if (total >= 70) return "muy";
        if (total >= 50) return "buena";
        if (total >= 30) return "parcial";
        return "poca";
    }

    private static List<PuntuacionCompatibilidad> todasLasParejas() {
        return todasLasParejas(gente);
    }

    private static List<PuntuacionCompatibilidad> todasLasParejas(List<PerfilDeMatch> quienes) {
        List<PuntuacionCompatibilidad> puntuaciones = new ArrayList<>();
        for (int i = 0; i < quienes.size(); i++) {
            for (int j = i + 1; j < quienes.size(); j++) {
                puntuaciones.add(CalculadoraCompatibilidad.calcular(quienes.get(i), quienes.get(j)));
            }
        }
        return puntuaciones;
    }

    /** Cuantas parejas cambian de tramo entre dos configuraciones del motor. */
    private static int tramosQueCambian(List<PuntuacionCompatibilidad> antes,
                                        List<PuntuacionCompatibilidad> despues) {
        int cambios = 0;
        for (int i = 0; i < antes.size(); i++) {
            if (!tramo(antes.get(i).total()).equals(tramo(despues.get(i).total()))) cambios++;
        }
        return cambios;
    }

    private static PesosDelMotor sin(String factor) {
        return switch (factor) {
            case "horario" -> new PesosDelMotor(0, 10, 10, 12, 10, 5, 8, 5);
            case "nivel" -> new PesosDelMotor(40, 0, 10, 12, 10, 5, 8, 5);
            case "fuerza" -> new PesosDelMotor(40, 10, 0, 12, 10, 5, 8, 5);
            case "objetivo" -> new PesosDelMotor(40, 10, 10, 0, 10, 5, 8, 5);
            case "constancia" -> new PesosDelMotor(40, 10, 10, 12, 0, 5, 8, 5);
            case "rutina" -> new PesosDelMotor(40, 10, 10, 12, 10, 0, 8, 5);
            case "gimnasio" -> new PesosDelMotor(40, 10, 10, 12, 10, 5, 0, 5);
            case "edad" -> new PesosDelMotor(40, 10, 10, 12, 10, 5, 8, 0);
            default -> throw new IllegalArgumentException("Factor desconocido: " + factor);
        };
    }

    /**
     * El informe: que porcentaje de parejas cambia de tramo al anular cada peso.
     *
     * <p>Se imprime a proposito. Los numeros exactos no se afirman aqui —son el
     * resultado, y cambian si cambia el reparto— pero quedan a la vista de quien
     * ejecute la suite, que es donde tienen que estar para poder citarlos.
     */
    @Test
    @DisplayName("Informe: cuanto mueve cada factor las decisiones del motor")
    void informeDeSensibilidad() {
        CalculadoraCompatibilidad.configurar(DE_FABRICA);
        List<PuntuacionCompatibilidad> base = todasLasParejas();

        Map<String, Integer> movidas = new LinkedHashMap<>();
        for (String factor : List.of("horario", "objetivo", "nivel", "fuerza",
                                     "constancia", "gimnasio", "rutina", "edad")) {
            CalculadoraCompatibilidad.configurar(sin(factor));
            movidas.put(factor, tramosQueCambian(base, todasLasParejas()));
        }
        CalculadoraCompatibilidad.configurar(DE_FABRICA);

        Map<String, Integer> pesos = Map.of("horario", 40, "objetivo", 12, "nivel", 10,
                "fuerza", 10, "constancia", 10, "gimnasio", 8, "rutina", 5, "edad", 5);

        System.out.println();
        System.out.println("=== Sensibilidad del motor ===");
        System.out.printf("Poblacion: %d perfiles, %d parejas (semilla %d)%n",
                CUANTOS, base.size(), BancoDePerfiles.SEMILLA);
        System.out.println();
        System.out.println("  factor        peso   mueve   sin datos   rinde");
        System.out.println("  ------------------------------------------------");

        movidas.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> {
                    double mueve = 100.0 * e.getValue() / base.size();
                    double sinDatos = 100.0 * vecesSinDatos(base, e.getKey()) / base.size();
                    // Cuanto mueve por punto de peso: separa el factor que
                    // decide poco porque pesa poco del que decide poco a pesar
                    // de pesar mucho, que es otro problema distinto.
                    double rinde = mueve / pesos.get(e.getKey());

                    System.out.printf("  %-12s %4d  %5.1f %%   %5.1f %%    %.2f%n",
                            e.getKey(), pesos.get(e.getKey()), mueve, sinDatos, rinde);
                });
        System.out.println();
        System.out.println("  mueve     = parejas que cambian de tramo si se anula el factor");
        System.out.println("  sin datos = parejas en las que el factor no se puede evaluar");
        System.out.println("  rinde     = puntos de 'mueve' por cada punto de peso");
        System.out.println();

        assertEquals(8, movidas.size());
    }

    /**
     * En cuantas parejas un factor no se puede evaluar.
     *
     * <p>Es la mitad que falta del analisis. Un factor puede mover pocas
     * decisiones por dos motivos que no se parecen en nada: porque pesa poco —y
     * entonces esta bien— o porque casi nunca hay datos con los que evaluarlo, y
     * entonces sus puntos se estan repartiendo entre los demas la mayor parte
     * del tiempo. Lo segundo no se arregla subiendole el peso.
     */
    /**
     * La misma tabla, con la gente entrenando a las horas de verdad.
     *
     * <h2>Por que hace falta</h2>
     *
     * <p>La tabla de arriba se mide sobre una poblacion que reparte las horas de
     * entrenamiento uniformemente entre las 7 y las 19. Eso produce un 16,9 % de
     * parejas que coinciden en algo, y la unica referencia que existe —los
     * quince perfiles escritos a mano de la demostracion— da un <b>50,5 %</b>:
     * diecisiete de sus treinta y una franjas empiezan a las 18:00.
     *
     * <p>Un factor que solo puede evaluarse cuando hay solape sale medido a la
     * baja en la poblacion uniforme, y la pregunta es cuanto. Esto lo contesta
     * repitiendo la medicion entera sobre una poblacion apilada en las horas
     * habituales.
     *
     * <p>Quince perfiles a mano no son una muestra, asi que esto <b>no</b> dice
     * cual de las dos poblaciones es la buena. Dice de que supuesto depende cada
     * numero, que es lo que se puede saber sin usuarios reales.
     */
    @Test
    @DisplayName("Informe: cuanto de la tabla depende de a que hora entrena la gente")
    void informeSegunLosHorarios() {
        List<PerfilDeMatch> punta = BancoDePerfiles.poblacion(
                CUANTOS, true, BancoDePerfiles.RepartoDeEjercicios.COMO_HOY,
                BancoDePerfiles.RepartoDeGimnasios.COMO_HOY,
                BancoDePerfiles.Desplazamiento.COMO_HOY,
                BancoDePerfiles.RepartoDeHorarios.HORAS_PUNTA);

        List<String> factores = List.of("horario", "objetivo", "nivel", "fuerza",
                                        "constancia", "gimnasio", "rutina", "edad");

        Map<String, Integer> uniforme = mueveCadaFactor(gente, factores);
        Map<String, Integer> apilada = mueveCadaFactor(punta, factores);
        int total = uniforme.size() > 0 ? todasLasParejas(gente).size() : 1;

        System.out.println();
        System.out.println("=== La tabla, segun a que hora entrene la gente ===");
        System.out.println();
        System.out.println("  factor        uniforme   horas punta   sin datos (unif -> punta)");
        System.out.println("  --------------------------------------------------------------");

        CalculadoraCompatibilidad.configurar(DE_FABRICA);
        List<PuntuacionCompatibilidad> baseU = todasLasParejas(gente);
        List<PuntuacionCompatibilidad> baseP = todasLasParejas(punta);

        for (String factor : factores) {
            System.out.printf("  %-12s   %5.1f %%      %5.1f %%       %4.1f %% -> %4.1f %%%n",
                    factor,
                    100.0 * uniforme.get(factor) / total,
                    100.0 * apilada.get(factor) / total,
                    100.0 * vecesSinDatos(baseU, factor) / total,
                    100.0 * vecesSinDatos(baseP, factor) / total);
        }
        System.out.println();

        assertEquals(factores.size(), apilada.size());
    }

    private static Map<String, Integer> mueveCadaFactor(List<PerfilDeMatch> quienes,
                                                        List<String> factores) {
        CalculadoraCompatibilidad.configurar(DE_FABRICA);
        List<PuntuacionCompatibilidad> base = todasLasParejas(quienes);

        Map<String, Integer> movidas = new LinkedHashMap<>();
        for (String factor : factores) {
            CalculadoraCompatibilidad.configurar(sin(factor));
            movidas.put(factor, tramosQueCambian(base, todasLasParejas(quienes)));
        }
        CalculadoraCompatibilidad.configurar(DE_FABRICA);
        return movidas;
    }

    private static int vecesSinDatos(List<PuntuacionCompatibilidad> puntuaciones, String factor) {
        return (int) puntuaciones.stream()
                .filter(p -> p.factoresSinDatos().contains(factor))
                .count();
    }

    /**
     * El horario vale 40 de 100 y el motor entero se apoya en el: si dos
     * personas no coinciden, no entrenan juntas, y ningun otro factor arregla
     * eso. Si anularlo no fuera lo que mas mueve, el reparto estaria mintiendo.
     */
    @Test
    @DisplayName("El horario es, de largo, lo que mas decide")
    void elHorarioManda() {
        CalculadoraCompatibilidad.configurar(DE_FABRICA);
        List<PuntuacionCompatibilidad> base = todasLasParejas();

        CalculadoraCompatibilidad.configurar(sin("horario"));
        int sinHorario = tramosQueCambian(base, todasLasParejas());

        CalculadoraCompatibilidad.configurar(sin("objetivo"));
        int sinObjetivo = tramosQueCambian(base, todasLasParejas());

        assertTrue(sinHorario > sinObjetivo,
                "Anular el horario mueve " + sinHorario + " parejas y anular el objetivo "
                        + sinObjetivo + ": el factor que mas pesa tiene que ser el que mas decide");
    }

    /**
     * Cinco puntos sobre cien es poco por definicion, y la edad esta puesta
     * como desempate. Lo que se comprueba es que sigue siendo un desempate y no
     * se ha convertido en algo que mueva la pantalla por su cuenta.
     */
    @Test
    @DisplayName("La edad desempata, no decide")
    void laEdadSoloDesempata() {
        CalculadoraCompatibilidad.configurar(DE_FABRICA);
        List<PuntuacionCompatibilidad> base = todasLasParejas();

        CalculadoraCompatibilidad.configurar(sin("edad"));
        double porcentaje = 100.0 * tramosQueCambian(base, todasLasParejas()) / base.size();

        assertTrue(porcentaje < 15.0,
                "Anular la edad cambia el tramo del " + porcentaje + " % de las parejas; "
                        + "para un factor de desempate eso es demasiado");
    }

    /**
     * La fuerza es el factor con menos cobertura, y no por casualidad.
     *
     * <p>Los demas factores solo necesitan que las dos personas hayan rellenado
     * un campo. Este necesita algo mas fuerte: que hayan apuntado marcas <b>del
     * mismo ejercicio</b>. Dos personas con tres marcas cada una pueden no
     * compartir ninguna, y entonces no hay nada que comparar por mucho que las
     * dos hayan rellenado su perfil entero.
     *
     * <p>Eso tiene una consecuencia que no se ve leyendo la tabla de pesos: sus
     * 10 puntos no actuan sobre todas las parejas, sino sobre las pocas en las
     * que coinciden. Y no se arregla subiendole el peso — subir el peso de algo
     * que no se puede evaluar no lo hace mas influyente, solo reparte mas puntos
     * entre los demas cuando falta.
     *
     * <p><b>El numero exacto depende de esta poblacion</b>, donde los ejercicios
     * se reparten al azar entre seis. En la realidad la gente apunta los basicos
     * y coincidiran mas. Lo que no depende de la poblacion es el mecanismo, y
     * eso es lo que fija esta prueba.
     */
    @Test
    @DisplayName("La fuerza es el factor que menos veces se puede evaluar")
    void laFuerzaEsElQueMenosDatosTiene() {
        CalculadoraCompatibilidad.configurar(DE_FABRICA);
        List<PuntuacionCompatibilidad> base = todasLasParejas();

        int sinFuerza = vecesSinDatos(base, "fuerza");

        for (String otro : List.of("horario", "objetivo", "nivel", "constancia",
                                   "gimnasio", "rutina", "edad")) {
            assertTrue(sinFuerza > vecesSinDatos(base, otro),
                    "La fuerza deberia ser el factor con menos cobertura —exige coincidir en el "
                            + "mismo ejercicio, no solo rellenar un campo— pero '" + otro
                            + "' tiene menos datos todavia");
        }
    }

    /**
     * Ninguno de los ocho puede ser decorativo.
     *
     * <p>Un peso que no cambia ninguna decision es peor que no tenerlo: ocupa
     * sitio en la tabla del README, hay que explicarlo, y quien lo lea creera
     * que influye en algo. Si algun dia uno sale a cero, la respuesta no es
     * subirle el peso: es preguntarse si ese factor tiene datos alguna vez.
     */
    @Test
    @DisplayName("Ningun factor esta de adorno: todos mueven alguna decision")
    void ningunoEsDecorativo() {
        CalculadoraCompatibilidad.configurar(DE_FABRICA);
        List<PuntuacionCompatibilidad> base = todasLasParejas();

        for (String factor : List.of("horario", "objetivo", "nivel", "fuerza",
                                     "constancia", "gimnasio", "rutina", "edad")) {
            CalculadoraCompatibilidad.configurar(sin(factor));
            int cambios = tramosQueCambian(base, todasLasParejas());

            assertTrue(cambios > 0,
                    "Anular '" + factor + "' no cambia el tramo de ninguna pareja: "
                            + "o no tiene datos casi nunca, o su valor sale siempre igual");
        }
    }
}
