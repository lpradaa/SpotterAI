package com.spotterai.backend.matching;

import com.spotterai.backend.matching.BancoDePerfiles.Desplazamiento;
import com.spotterai.backend.matching.BancoDePerfiles.RepartoDeEjercicios;
import com.spotterai.backend.matching.BancoDePerfiles.RepartoDeGimnasios;
import com.spotterai.backend.matching.BancoDePerfiles.RepartoDeHorarios;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * De donde sale de verdad la influencia del gimnasio.
 *
 * <h2>Que pregunta responde</h2>
 *
 * <p>El gimnasio pesa 8 de 100, y en el analisis de sensibilidad sale rindiendo
 * 1,38 — muy por encima de su peso, casi como el horario que pesa 40. Ese numero
 * pedia una explicacion y se le dio una: que ademas de sus puntos, el gimnasio
 * decide cuanto vale el solape horario.
 *
 * <p>Pero esa explicacion <b>no estaba medida</b>. {@code SensibilidadDelMotorTest}
 * anula factores bajando su peso, y bajar el peso del gimnasio a cero no toca el
 * descuento del horario: eso son constantes de {@link CalculadoraCompatibilidad},
 * no un peso. Asi que el 11,1 % de la tabla es una sola de las dos vias, y la
 * frase que lo explicaba hablaba de la otra.
 *
 * <h2>Las dos vias</h2>
 *
 * <ul>
 *   <li><b>A, los puntos.</b> Compartir sala da 8 de 100. Se anula con el peso.
 *   <li><b>B, el descuento del horario.</b> Con salas distintas el solape cuenta
 *       una cuarta parte —0,60 si alguien se desplaza— porque coincidir a las
 *       seis en dos edificios distintos no es coincidir.
 * </ul>
 *
 * <h2>El resultado, que desmiente la explicacion que se habia dado</h2>
 *
 * <p>Parecia evidente que B tenia que mover mas: multiplica un factor de 40
 * puntos, mientras que A reparte 8. Sale al reves —11,1 % contra 2,5 %— y el
 * motivo es que la base grande no sirve de nada si casi siempre vale cero.
 *
 * <p>Solo el 16,9 % de las parejas coinciden en algun horario, y de esas, las
 * que ademas estan en salas distintas son el 10,1 % del total. <b>Multiplicar
 * cero por 0,25 sigue dando cero</b>: en las nueve de cada diez parejas
 * restantes, compartir sala o no da exactamente igual por esta via.
 *
 * <p>A, en cambio, actua sobre <b>todas</b>: comparten sala o no, siempre hay
 * 8 puntos que dar o no dar, y ese salto binario cruza umbrales de tramo a
 * menudo. Ahi esta su 1,38 de rendimiento, y no donde se dijo.
 *
 * <p>Visto de otro modo: donde B <i>puede</i> actuar cambia una decision de cada
 * cuatro (2,5 de 10,1), que es mucho. Lo que pasa es que casi nunca puede.
 *
 * <h2>Como se separan sin tocar el motor</h2>
 *
 * <p>No hace falta hacer configurable el descuento: basta con mover los datos.
 * Con el peso del gimnasio ya a cero, poner a toda la poblacion en la misma sala
 * deja el multiplicador en 1 para todas las parejas, y no cambia nada mas. La
 * diferencia entre esas dos medidas es la via B en estado puro.
 *
 * <p>Es el mismo truco que se uso para medir la afinidad: quitarle los datos al
 * factor en vez de quitarlo del codigo.
 */
class LasDosViasDelGimnasioTest {

    private static final int CUANTOS = 60;

    /** Los de fabrica. El gimnasio es el septimo: 8. */
    private static final PesosDelMotor CON_LOS_8_PUNTOS =
            new PesosDelMotor(40, 10, 10, 12, 10, 5, 8, 5);

    /** Sin la via A: solo queda el descuento del horario. */
    private static final PesosDelMotor SIN_LOS_8_PUNTOS =
            new PesosDelMotor(40, 10, 10, 12, 10, 5, 0, 5);

    /** La gente de siempre, repartida en tres salas. */
    private static final List<PerfilDeMatch> REPARTIDOS = BancoDePerfiles.poblacion(
            CUANTOS, true, RepartoDeEjercicios.COMO_HOY,
            RepartoDeGimnasios.COMO_HOY, Desplazamiento.COMO_HOY);

    /** La misma gente, toda en la misma sala. */
    private static final List<PerfilDeMatch> TODOS_JUNTOS = BancoDePerfiles.poblacion(
            CUANTOS, true, RepartoDeEjercicios.COMO_HOY,
            RepartoDeGimnasios.TODOS_JUNTOS, Desplazamiento.COMO_HOY);

    /** La misma gente, sin que nadie diga que se desplaza. */
    private static final List<PerfilDeMatch> NADIE_SE_MUEVE = BancoDePerfiles.poblacion(
            CUANTOS, true, RepartoDeEjercicios.COMO_HOY,
            RepartoDeGimnasios.COMO_HOY, Desplazamiento.NADIE_SE_MUEVE);

    /** La misma tirada, pero con la gente entrenando a las horas de siempre. */
    private static final List<PerfilDeMatch> PUNTA = BancoDePerfiles.poblacion(
            CUANTOS, true, RepartoDeEjercicios.COMO_HOY,
            RepartoDeGimnasios.COMO_HOY, Desplazamiento.COMO_HOY,
            RepartoDeHorarios.HORAS_PUNTA);

    private static final List<PerfilDeMatch> PUNTA_TODOS_JUNTOS = BancoDePerfiles.poblacion(
            CUANTOS, true, RepartoDeEjercicios.COMO_HOY,
            RepartoDeGimnasios.TODOS_JUNTOS, Desplazamiento.COMO_HOY,
            RepartoDeHorarios.HORAS_PUNTA);

    @AfterEach
    void devolverLosPesos() {
        CalculadoraCompatibilidad.configurar(CON_LOS_8_PUNTOS);
    }

    // ------------------------------------------------------------------ medir

    private static List<PuntuacionCompatibilidad> parejas(List<PerfilDeMatch> gente,
                                                         PesosDelMotor pesos) {
        CalculadoraCompatibilidad.configurar(pesos);

        List<PuntuacionCompatibilidad> puntuaciones = new ArrayList<>();
        for (int i = 0; i < gente.size(); i++) {
            for (int j = i + 1; j < gente.size(); j++) {
                puntuaciones.add(CalculadoraCompatibilidad.calcular(gente.get(i), gente.get(j)));
            }
        }
        return puntuaciones;
    }

    private static String tramo(int total) {
        if (total >= 85) return "excelente";
        if (total >= 70) return "muy";
        if (total >= 50) return "buena";
        if (total >= 30) return "parcial";
        return "poca";
    }

    private static String dominante(PuntuacionCompatibilidad p) {
        FactorCompatibilidad f = p.factorDominante();
        return f == null ? "ninguno" : f.nombre();
    }

    private static int tramosQueCambian(List<PuntuacionCompatibilidad> a,
                                        List<PuntuacionCompatibilidad> b) {
        int cambios = 0;
        for (int i = 0; i < a.size(); i++) {
            if (!tramo(a.get(i).total()).equals(tramo(b.get(i).total()))) cambios++;
        }
        return cambios;
    }

    private static int frasesQueCambian(List<PuntuacionCompatibilidad> a,
                                        List<PuntuacionCompatibilidad> b) {
        int cambios = 0;
        for (int i = 0; i < a.size(); i++) {
            if (!dominante(a.get(i)).equals(dominante(b.get(i)))) cambios++;
        }
        return cambios;
    }

    private static double diferenciaMedia(List<PuntuacionCompatibilidad> a,
                                          List<PuntuacionCompatibilidad> b) {
        double suma = 0;
        for (int i = 0; i < a.size(); i++) {
            suma += Math.abs(a.get(i).total() - b.get(i).total());
        }
        return suma / a.size();
    }

    // ---------------------------------------------------------------- informe

    @Test
    @DisplayName("Informe: cuanto mueve cada una de las dos vias del gimnasio")
    void informe() {
        // Via A: los 8 puntos, sobre la poblacion de siempre.
        List<PuntuacionCompatibilidad> conPuntos = parejas(REPARTIDOS, CON_LOS_8_PUNTOS);
        List<PuntuacionCompatibilidad> sinPuntos = parejas(REPARTIDOS, SIN_LOS_8_PUNTOS);

        // Via B: el descuento del horario, ya sin los puntos por medio.
        List<PuntuacionCompatibilidad> sinPuntosJuntos = parejas(TODOS_JUNTOS, SIN_LOS_8_PUNTOS);

        // Las dos a la vez: el gimnasio dejando de influir del todo.
        List<PuntuacionCompatibilidad> sinNada = sinPuntosJuntos;

        int total = conPuntos.size();

        System.out.println();
        System.out.println("=== Las dos vias del gimnasio ===");
        System.out.printf("Poblacion: %d perfiles, %d parejas (semilla %d)%n",
                CUANTOS, total, BancoDePerfiles.SEMILLA);
        System.out.println();
        System.out.println("  via                              tramos   frases   dif. media");
        System.out.println("  --------------------------------------------------------------");
        System.out.printf("  A  los 8 puntos                  %5.1f %%  %5.1f %%   %5.2f%n",
                pct(tramosQueCambian(conPuntos, sinPuntos), total),
                pct(frasesQueCambian(conPuntos, sinPuntos), total),
                diferenciaMedia(conPuntos, sinPuntos));
        System.out.printf("  B  el descuento del horario      %5.1f %%  %5.1f %%   %5.2f%n",
                pct(tramosQueCambian(sinPuntos, sinPuntosJuntos), total),
                pct(frasesQueCambian(sinPuntos, sinPuntosJuntos), total),
                diferenciaMedia(sinPuntos, sinPuntosJuntos));
        System.out.printf("  A+B el gimnasio entero           %5.1f %%  %5.1f %%   %5.2f%n",
                pct(tramosQueCambian(conPuntos, sinNada), total),
                pct(frasesQueCambian(conPuntos, sinNada), total),
                diferenciaMedia(conPuntos, sinNada));
        System.out.println();

        // Y la pregunta de producto que cuelga de la via B.
        List<PuntuacionCompatibilidad> nadieSeMueve = parejas(NADIE_SE_MUEVE, CON_LOS_8_PUNTOS);
        System.out.printf("  Preguntar «puedo desplazarme»    %5.1f %%  %5.1f %%   %5.2f%n",
                pct(tramosQueCambian(conPuntos, nadieSeMueve), total),
                pct(frasesQueCambian(conPuntos, nadieSeMueve), total),
                diferenciaMedia(conPuntos, nadieSeMueve));
        System.out.println();

        repartoDeSalas(total);

        System.out.println("  tramos = parejas que cambian de tramo  |  frases = de factor dominante");
        System.out.println("  dif. media = puntos de diferencia, de 100");
        System.out.println();

        assertTrue(total > 0);
    }

    /** En que rama cae cada pareja, que es el contexto de todo lo de arriba. */
    private static void repartoDeSalas(int total) {
        int mismas = 0;
        int distintasConSalida = 0;
        int distintas = 0;

        for (int i = 0; i < REPARTIDOS.size(); i++) {
            for (int j = i + 1; j < REPARTIDOS.size(); j++) {
                var uno = REPARTIDOS.get(i).usuario();
                var otro = REPARTIDOS.get(j).usuario();

                boolean misma = uno.getGimnasio() != null && otro.getGimnasio() != null
                        && uno.getGimnasio().getId().equals(otro.getGimnasio().getId());

                if (misma) mismas++;
                else if (uno.isPuedoDesplazarme() || otro.isPuedoDesplazarme()) distintasConSalida++;
                else distintas++;
            }
        }

        System.out.printf("  Reparto: misma sala %.1f %% | distinta con alguien dispuesto %.1f %% "
                        + "| distinta %.1f %%%n%n",
                pct(mismas, total), pct(distintasConSalida, total), pct(distintas, total));
    }

    private static double pct(int cuantas, int total) {
        return 100.0 * cuantas / total;
    }

    /** Cuantas parejas coinciden en algun horario, en la poblacion que sea. */
    private static int conSolape(List<PerfilDeMatch> gente) {
        int cuantas = 0;
        for (int i = 0; i < gente.size(); i++) {
            for (int j = i + 1; j < gente.size(); j++) {
                if (!CalculadoraSolape.calcular(
                        gente.get(i).horarios(), gente.get(j).horarios()).franjas().isEmpty()) {
                    cuantas++;
                }
            }
        }
        return cuantas;
    }

    /**
     * Informe: cuanto de lo anterior depende de a que hora entrena la gente.
     *
     * <p>La via B solo actua donde hay solape, asi que su medida hereda entera
     * la suposicion de horarios del banco — que reparte las horas de forma
     * uniforme, cosa que nadie hace. Aqui se repite todo sobre una poblacion
     * apilada en las horas de siempre, como la de la demostracion.
     *
     * <p>Las dos poblaciones <b>no son la misma gente</b> y no se comparan
     * pareja a pareja: lo que se compara es la conclusion.
     */
    @Test
    @DisplayName("Informe: lo mismo, con la gente entrenando a las horas de verdad")
    void informeConHorasPunta() {
        List<PuntuacionCompatibilidad> conPuntos = parejas(PUNTA, CON_LOS_8_PUNTOS);
        List<PuntuacionCompatibilidad> sinPuntos = parejas(PUNTA, SIN_LOS_8_PUNTOS);
        List<PuntuacionCompatibilidad> sinNada = parejas(PUNTA_TODOS_JUNTOS, SIN_LOS_8_PUNTOS);

        int total = conPuntos.size();

        System.out.println();
        System.out.println("=== Las dos vias, con la gente en horas punta ===");
        System.out.printf("  Coinciden en horario: %.1f %% (uniforme: %.1f %% | demostracion: 50,5 %%)%n%n",
                pct(conSolape(PUNTA), total), pct(conSolape(REPARTIDOS), total));

        System.out.println("  via                              tramos   frases   dif. media");
        System.out.println("  --------------------------------------------------------------");
        System.out.printf("  A  los 8 puntos                  %5.1f %%  %5.1f %%   %5.2f%n",
                pct(tramosQueCambian(conPuntos, sinPuntos), total),
                pct(frasesQueCambian(conPuntos, sinPuntos), total),
                diferenciaMedia(conPuntos, sinPuntos));
        System.out.printf("  B  el descuento del horario      %5.1f %%  %5.1f %%   %5.2f%n",
                pct(tramosQueCambian(sinPuntos, sinNada), total),
                pct(frasesQueCambian(sinPuntos, sinNada), total),
                diferenciaMedia(sinPuntos, sinNada));
        System.out.printf("  A+B el gimnasio entero           %5.1f %%  %5.1f %%   %5.2f%n",
                pct(tramosQueCambian(conPuntos, sinNada), total),
                pct(frasesQueCambian(conPuntos, sinNada), total),
                diferenciaMedia(conPuntos, sinNada));
        System.out.println();

        assertTrue(conSolape(PUNTA) > conSolape(REPARTIDOS),
                "Apilar a la gente en las mismas horas tiene que producir mas coincidencias "
                        + "que repartirlas por todo el dia; si no, el reparto no esta apilando nada");
    }

    // ------------------------------------------------- validar el instrumento

    /**
     * Que las tres poblaciones sean <b>la misma gente</b>.
     *
     * <p>Esto no valida el motor: valida el experimento. Ya paso una vez en este
     * banco —un cortocircuito se salto un sorteo, la secuencia se desplazo y dos
     * poblaciones que debian ser identicas resultaron ser dos grupos distintos—
     * y la medicion salio imposible sin que nada fallara.
     *
     * <p>Si esto se rompe, los porcentajes de arriba no miden el gimnasio: miden
     * la diferencia entre dos poblaciones cualesquiera.
     */
    @Test
    @DisplayName("Las tres poblaciones son la misma gente, y solo cambia lo que debe")
    void elExperimentoSeSostiene() {
        for (int i = 0; i < CUANTOS; i++) {
            var normal = REPARTIDOS.get(i).usuario();
            var juntos = TODOS_JUNTOS.get(i).usuario();
            var quietos = NADIE_SE_MUEVE.get(i).usuario();

            // Lo que no puede cambiar en ninguna de las dos variantes.
            for (var otro : List.of(juntos, quietos)) {
                assertEquals(normal.getNivel(), otro.getNivel(), "nivel del perfil " + i);
                assertEquals(normal.getObjetivos(), otro.getObjetivos(), "objetivo del perfil " + i);
                assertEquals(normal.getRutina(), otro.getRutina(), "rutina del perfil " + i);
                assertEquals(normal.getEdad(), otro.getEdad(), "edad del perfil " + i);
            }
            assertEquals(REPARTIDOS.get(i).horarios().size(), TODOS_JUNTOS.get(i).horarios().size(),
                    "horarios del perfil " + i);

            // Y lo que si tiene que cambiar, cada uno en su variante.
            assertEquals(1L, juntos.getGimnasio().getId(), "en TODOS_JUNTOS nadie sale de la sala 1");
            assertEquals(normal.isPuedoDesplazarme(), juntos.isPuedoDesplazarme(),
                    "cambiar de sala no puede tocar quien se desplaza");
            assertFalse(quietos.isPuedoDesplazarme(), "en NADIE_SE_MUEVE no se mueve nadie");
            assertEquals(normal.getGimnasio().getId(), quietos.getGimnasio().getId(),
                    "quitar el desplazamiento no puede tocar la sala");
        }
    }

    /**
     * La via B depende de los horarios y la A no, que es lo que las distingue.
     *
     * <p>Esta si es una propiedad y no un resultado: A da 8 puntos o no los da,
     * pase lo que pase con los horarios; B multiplica un solape, asi que solo
     * existe donde hay solape. Apilar a la gente en las mismas horas tiene que
     * amplificar B mucho mas que A.
     *
     * <p>Lo que <b>no</b> se fija es cual de las dos gana. En el reparto
     * uniforme gana A; en el apilado, B gana en frases y casi empata en tramos.
     * Eso es un resultado de cada poblacion, y ponerlo como prueba habria dejado
     * la suite en rojo en cuanto se midio la segunda.
     */
    @Test
    @DisplayName("Apilar los horarios amplifica la via B mucho mas que la A")
    void losHorariosSoloAmplificanUnaDeLasDos() {
        int aUniforme = tramosQueCambian(
                parejas(REPARTIDOS, CON_LOS_8_PUNTOS), parejas(REPARTIDOS, SIN_LOS_8_PUNTOS));
        int bUniforme = tramosQueCambian(
                parejas(REPARTIDOS, SIN_LOS_8_PUNTOS), parejas(TODOS_JUNTOS, SIN_LOS_8_PUNTOS));

        int aPunta = tramosQueCambian(
                parejas(PUNTA, CON_LOS_8_PUNTOS), parejas(PUNTA, SIN_LOS_8_PUNTOS));
        int bPunta = tramosQueCambian(
                parejas(PUNTA, SIN_LOS_8_PUNTOS), parejas(PUNTA_TODOS_JUNTOS, SIN_LOS_8_PUNTOS));

        double creceA = (double) aPunta / aUniforme;
        double creceB = (double) bPunta / bUniforme;

        assertTrue(creceB > creceA * 2,
                "Con la gente apilada en las mismas horas, la via que multiplica el solape "
                        + "tiene que crecer mucho mas que la que reparte puntos fijos. "
                        + "A x" + String.format("%.2f", creceA)
                        + " B x" + String.format("%.2f", creceB));
    }

    /**
     * Ninguna de las dos vias es decoracion, y juntas mandan mas que cada una.
     *
     * <p>Esto si es una propiedad y no un resultado: los porcentajes de arriba
     * cambiaran si cambia el reparto de horarios o el numero de salas, pero que
     * el gimnasio influya por dos caminos distintos y que los dos hagan algo no
     * deberia cambiar sin que alguien lo decida.
     *
     * <p>Lo que <b>no</b> se fija aqui es «A mueve mas que B». Es cierto en esta
     * poblacion y por un motivo que se entiende —B necesita solape y casi no lo
     * hay— pero en una poblacion con horarios mas apretados podria darse la
     * vuelta. Convertir ese resultado en prueba seria confundir lo que se ha
     * medido con lo que tiene que pasar.
     */
    @Test
    @DisplayName("Las dos vias hacen algo, y el gimnasio entero mueve mas que cualquiera de ellas")
    void ningunaDeLasDosViasEsDecorativa() {
        List<PuntuacionCompatibilidad> conPuntos = parejas(REPARTIDOS, CON_LOS_8_PUNTOS);
        List<PuntuacionCompatibilidad> sinPuntos = parejas(REPARTIDOS, SIN_LOS_8_PUNTOS);
        List<PuntuacionCompatibilidad> sinNada = parejas(TODOS_JUNTOS, SIN_LOS_8_PUNTOS);

        int soloA = tramosQueCambian(conPuntos, sinPuntos);
        int soloB = tramosQueCambian(sinPuntos, sinNada);
        int lasDos = tramosQueCambian(conPuntos, sinNada);

        assertTrue(soloA > 0, "Los 8 puntos del gimnasio no cambian ninguna decision");
        assertTrue(soloB > 0,
                "El descuento del horario por no compartir sala no cambia ninguna decision: "
                        + "o no hay parejas con solape en salas distintas, o el descuento dejo "
                        + "de aplicarse");

        assertTrue(lasDos >= soloA && lasDos >= soloB,
                "Quitarle al gimnasio las dos vias tiene que mover al menos tanto como quitarle "
                        + "una sola. Si no, el experimento esta midiendo otra cosa: A=" + soloA
                        + " B=" + soloB + " A+B=" + lasDos);
    }

    /**
     * Los 8 puntos no pueden mover la puntuacion mas de 8.
     *
     * <p>La otra comprobacion del instrumento, la misma que protege el analisis
     * de sensibilidad. Un factor que al anularse mueve mas de lo que vale
     * significa que se ha colado otra diferencia por el camino.
     */
    @Test
    @DisplayName("Anular los 8 puntos no mueve la puntuacion mas de 8")
    void laViaANoPuedeMoverMasDeLoQueVale() {
        List<PuntuacionCompatibilidad> conPuntos = parejas(REPARTIDOS, CON_LOS_8_PUNTOS);
        List<PuntuacionCompatibilidad> sinPuntos = parejas(REPARTIDOS, SIN_LOS_8_PUNTOS);

        int mayorSalto = 0;
        for (int i = 0; i < conPuntos.size(); i++) {
            mayorSalto = Math.max(mayorSalto,
                    Math.abs(conPuntos.get(i).total() - sinPuntos.get(i).total()));
        }

        assertTrue(mayorSalto <= 8,
                "Quitar un factor de 8 puntos ha movido " + mayorSalto + ": por ahi se esta "
                        + "colando alguna diferencia que no es el peso del gimnasio");
    }

    /**
     * Por que la via B mueve tan poco: casi nadie coincide en horario.
     *
     * <p>El descuento multiplica el solape, y multiplicar cero por lo que sea
     * sigue dando cero. En las parejas sin ni un minuto en comun —que son la
     * inmensa mayoria— compartir sala o no da exactamente igual, asi que la via
     * B solo puede actuar sobre la fraccion que ademas coincide.
     */
    @Test
    @DisplayName("La via B solo puede actuar donde hay solape que descontar")
    void laViaBNecesitaSolape() {
        CalculadoraCompatibilidad.configurar(CON_LOS_8_PUNTOS);

        int conSolape = 0;
        int conSolapeYSalaDistinta = 0;

        for (int i = 0; i < REPARTIDOS.size(); i++) {
            for (int j = i + 1; j < REPARTIDOS.size(); j++) {
                var uno = REPARTIDOS.get(i);
                var otro = REPARTIDOS.get(j);

                boolean coinciden = !CalculadoraSolape.calcular(
                        uno.horarios(), otro.horarios()).franjas().isEmpty();
                if (!coinciden) continue;

                conSolape++;
                if (!uno.usuario().getGimnasio().getId()
                        .equals(otro.usuario().getGimnasio().getId())) {
                    conSolapeYSalaDistinta++;
                }
            }
        }

        int total = REPARTIDOS.size() * (REPARTIDOS.size() - 1) / 2;
        System.out.println();
        System.out.printf("  Parejas que coinciden en horario:            %5.1f %%%n",
                pct(conSolape, total));
        System.out.printf("  De ellas, en salas distintas (donde actua B): %5.1f %% del total%n%n",
                pct(conSolapeYSalaDistinta, total));

        assertTrue(conSolapeYSalaDistinta > 0,
                "Sin ninguna pareja que coincida en horario y no comparta sala, la via B "
                        + "no tendria donde actuar y su medida no significaria nada");
    }
}
