package com.spotterai.backend.matching;

import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.models.Levantamiento;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.semantica.VectorDePrueba;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Una poblacion de perfiles para medir el motor, no para probarlo.
 *
 * <h2>Que es y que no es</h2>
 *
 * <p>Esto <b>no valida</b> que el motor acierte. Validar significa comparar sus
 * predicciones con lo que hace la gente de verdad, y para eso esta el embudo de
 * {@code /embudo}, que hoy dice honestamente que no hay muestra suficiente para
 * concluir nada. Inventar usuarios y llamar a eso validacion seria justo el
 * teatro que este proyecto evita en todas partes.
 *
 * <p>Lo que si permite una poblacion sintetica es medir el motor <b>contra si
 * mismo</b>: cuanto mueve cada factor la puntuacion, en que zona el numero es
 * inestable, y si algun peso esta puesto donde no cambia ninguna decision. Eso
 * no depende de que los perfiles sean reales; depende de que sean variados.
 *
 * <h2>Por que es determinista</h2>
 *
 * <p>Semilla fija. Un analisis cuyos numeros cambian en cada ejecucion no se
 * puede citar en ningun sitio, y una prueba que afirme sobre ellos se pondria
 * roja sola cada tantas veces — que es la peor clase de prueba, la que enseña a
 * ignorar los fallos.
 *
 * <h2>Que puede faltar y que no</h2>
 *
 * <p>Esto se equivoco en la primera version y merece quedar escrito. La
 * poblacion generaba gente sin horarios, sin gimnasio, sin nivel y sin edad
 * "para ejercitar la redistribucion de pesos", y el analisis salio con un 41 %
 * de parejas sin horario que cruzar.
 *
 * <p>Esa gente no existe. {@link PerfilMinimo} exige horarios, gimnasio, nivel,
 * objetivo, rutina y edad, y el guardian del frontend no deja pasar a Explorar
 * sin ellos: nadie llega a ser puntuado sin los seis. Medir sobre perfiles que
 * la aplicacion no permite es medir otro motor.
 *
 * <p>Lo que si falta de verdad son los tres que {@code PerfilMinimo} deja
 * opcionales a proposito, y no por descuido:
 *
 * <ul>
 *   <li><b>las marcas</b>, porque exigirlas produciria numeros inventados por
 *       quien no los sabe, y entrarian derechos al factor del que depende el
 *       nombre del producto;</li>
 *   <li><b>la constancia</b>, que ni se rellena: se gana yendo;</li>
 *   <li><b>la biografia</b>, que es lo unico que se escribe con palabras.</li>
 * </ul>
 *
 * <p>Asi que la redistribucion de pesos se sigue ejercitando —esos tres faltan a
 * menudo— pero sobre la poblacion que el motor ve de verdad.
 */
public final class BancoDePerfiles {

    private BancoDePerfiles() {}

    /** Cambiarla cambia todos los numeros del analisis: es parte del resultado. */
    public static final long SEMILLA = 20260817L;

    private static final String[] NIVELES = {"Principiante", "Intermedio", "Avanzado"};
    private static final String[] OBJETIVOS = {"Hipertrofia", "Fuerza", "Pérdida de peso", "Resistencia"};
    private static final Rutina[] RUTINAS = Rutina.values();
    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    /**
     * Tres gimnasios y "ninguno".
     *
     * <p>Pocos y repetidos a proposito: con un gimnasio por persona nadie
     * compartiria sala y el factor que mas pesa —el horario, que se descuenta a
     * una cuarta parte en gimnasios distintos— saldria siempre por su rama mala.
     */
    private static final int GIMNASIOS = 3;

    /**
     * Como reparte la gente sus marcas entre los seis ejercicios.
     *
     * <p>Es la variable del experimento sobre la cobertura de la fuerza. Hoy el
     * formulario ofrece los seis sin sugerir ninguno y la gente elige lo que
     * quiere; la pregunta es que pasaria si sugiriera los basicos.
     */
    public enum RepartoDeEjercicios {
        /** Lo de hoy: los seis por igual. */
        COMO_HOY,
        /** Cinco de cada seis marcas caen en sentadilla, banca o peso muerto. */
        SUGIRIENDO_LOS_BASICOS
    }

    /**
     * Donde entrena la gente, para poder separar las dos vias del gimnasio.
     *
     * <p>El gimnasio influye por dos caminos a la vez y solo uno se puede anular
     * bajando su peso. El otro esta en el horario: cuando no se comparte sala,
     * el solape se descuenta a una cuarta parte —o a 0,60 si alguien se
     * desplaza— y eso no lo toca {@code PesosDelMotor}.
     *
     * <p>Poner a todos en la misma sala neutraliza <b>solo</b> esa segunda via:
     * el multiplicador pasa a ser 1 para todas las parejas. Combinado con el
     * peso a cero, deja al gimnasio sin ninguna influencia, que es lo que
     * permite atribuir cuanto viene de cada camino.
     */
    public enum RepartoDeGimnasios {
        /** Lo de hoy: tres salas repartidas al azar. */
        COMO_HOY,
        /** Todos en la misma: el horario nunca se descuenta por la sala. */
        TODOS_JUNTOS
    }

    /**
     * Quien esta dispuesto a moverse.
     *
     * <p>La otra variable de la misma familia: marcarlo sube el descuento de
     * 0,25 a 0,60, asi que es una decision de producto —una pregunta mas en el
     * formulario— cuyo efecto nunca se habia contado.
     */
    public enum Desplazamiento {
        /** Lo de hoy: uno de cada cuatro dice que se mueve. */
        COMO_HOY,
        /** Nadie lo marca, que es lo que pasaria si no se preguntara. */
        NADIE_SE_MUEVE
    }

    /**
     * A que horas entrena la gente.
     *
     * <p>La version original reparte las horas de inicio uniformemente entre las
     * 7 y las 19, y los dias entre los siete de la semana. Eso da un 16,9 % de
     * parejas que coinciden en algo — y ese numero condiciona todo lo que
     * dependa del solape, empezando por el descuento del gimnasio, que solo
     * puede actuar donde hay horario que descontar.
     *
     * <p>La gente no entrena a horas repartidas. En la poblacion de la
     * demostracion, que es la unica escrita pensando en personas concretas, 17
     * de las 31 franjas empiezan a las 18:00 y solo 2 antes de las 8; los dias
     * se apilan en lunes y miercoles. Su tasa de coincidencia es del
     * <b>50,5 %</b>, tres veces la del banco.
     *
     * <p>{@link #HORAS_PUNTA} imita ese reparto. No es «mas realista» en el
     * sentido de estar validado —quince perfiles escritos a mano no son una
     * muestra— pero es la mejor referencia que hay, y sirve para lo que importa:
     * saber cuanto de lo medido depende de este supuesto.
     */
    public enum RepartoDeHorarios {
        /** Uniforme: cualquier hora entre las 7 y las 19, cualquier dia. */
        COMO_HOY,
        /** Apilado en la tarde y entre semana, como la demostracion. */
        HORAS_PUNTA
    }

    /**
     * Una poblacion de {@code cuantos} perfiles.
     *
     * @param conBiografia si se les pone biografia y vector. En false, el noveno
     *                     factor se queda sin datos para todos, que es como se
     *                     mide lo que aporta.
     */
    public static List<PerfilDeMatch> poblacion(int cuantos, boolean conBiografia) {
        return poblacion(cuantos, conBiografia, RepartoDeEjercicios.COMO_HOY);
    }

    /**
     * La misma poblacion, cambiando solo donde caen las marcas.
     *
     * <p>Se sortea siempre igual y lo unico que cambia es a que ejercicio se
     * traduce el numero sorteado: asi las dos poblaciones son la misma gente con
     * los mismos horarios, las mismas biografias y el mismo numero de marcas.
     * Si el reparto consumiera aleatoriedad distinta, la comparacion mediria dos
     * grupos distintos — que es el fallo que ya se cometio una vez aqui.
     */
    public static List<PerfilDeMatch> poblacion(int cuantos, boolean conBiografia,
                                                RepartoDeEjercicios reparto) {
        return poblacion(cuantos, conBiografia, reparto,
                RepartoDeGimnasios.COMO_HOY, Desplazamiento.COMO_HOY);
    }

    /**
     * La misma gente, moviendo donde entrenan o si estan dispuestos a viajar.
     *
     * <p>Vale lo mismo que para los ejercicios: los sorteos se consumen
     * <b>siempre</b> y lo unico que cambia es que se hace con el numero
     * sorteado. Si una variante se saltara una llamada a {@code azar}, la
     * secuencia se desplazaria y las dos poblaciones dejarian de ser la misma
     * gente — el fallo que ya se cometio aqui una vez.
     */
    public static List<PerfilDeMatch> poblacion(int cuantos, boolean conBiografia,
                                                RepartoDeEjercicios reparto,
                                                RepartoDeGimnasios salas,
                                                Desplazamiento desplazamiento) {
        return poblacion(cuantos, conBiografia, reparto, salas, desplazamiento,
                RepartoDeHorarios.COMO_HOY);
    }

    /**
     * La misma gente, cambiando ademas a que horas entrena.
     *
     * <p>Ojo con esta ultima: mover los horarios <b>si</b> cambia a la gente, no
     * solo una etiqueta. COMO_HOY y HORAS_PUNTA son dos poblaciones distintas a
     * proposito y no se comparan pareja a pareja — lo que se compara es lo que
     * mide el motor dentro de cada una. Las variantes de sala y desplazamiento
     * si siguen siendo la misma gente dentro de cada reparto de horarios, que es
     * lo que necesitan las mediciones del gimnasio.
     */
    public static List<PerfilDeMatch> poblacion(int cuantos, boolean conBiografia,
                                                RepartoDeEjercicios reparto,
                                                RepartoDeGimnasios salas,
                                                Desplazamiento desplazamiento,
                                                RepartoDeHorarios horas) {
        Random azar = new Random(SEMILLA);
        List<PerfilDeMatch> gente = new ArrayList<>(cuantos);

        for (int i = 0; i < cuantos; i++) {
            gente.add(perfil(i, azar, conBiografia, reparto, salas, desplazamiento, horas));
        }
        return gente;
    }

    private static PerfilDeMatch perfil(int i, Random azar, boolean conBiografia,
                                        RepartoDeEjercicios reparto,
                                        RepartoDeGimnasios salas,
                                        Desplazamiento desplazamiento,
                                        RepartoDeHorarios horas) {
        Usuario u = new Usuario();
        u.setId((long) i + 1);
        u.setNombre("Perfil " + (i + 1));
        u.setEmail("perfil" + (i + 1) + "@banco.test");

        // Los cinco que exige PerfilMinimo, mas el horario de abajo: quien no
        // los tiene no pasa del guardian, asi que aqui no puede faltar ninguno.
        u.setNivel(NIVELES[azar.nextInt(NIVELES.length)]);
        u.setObjetivos(OBJETIVOS[azar.nextInt(OBJETIVOS.length)]);
        u.setRutina(RUTINAS[azar.nextInt(RUTINAS.length)].name());
        u.setEdad(18 + azar.nextInt(35));
        boolean seMueve = azar.nextInt(4) == 0;
        u.setPuedoDesplazarme(desplazamiento == Desplazamiento.NADIE_SE_MUEVE ? false : seMueve);

        int sorteada = azar.nextInt(GIMNASIOS);
        Gimnasio g = new Gimnasio();
        g.setId(salas == RepartoDeGimnasios.TODOS_JUNTOS ? 1L : sorteada + 1L);
        g.setNombre("Gimnasio " + g.getId());
        u.setGimnasio(g);

        /*
         * La biografia es opcional de verdad, asi que uno de cada cinco no la
         * escribe ni siquiera en la poblacion "con biografia".
         *
         * El sorteo se consume SIEMPRE, aunque no se vaya a usar. Con
         * `conBiografia && azar.nextInt(5) != 0` el cortocircuito se salta la
         * llamada cuando conBiografia es false, la secuencia de numeros se
         * desplaza y las dos poblaciones dejan de ser la misma gente: son dos
         * grupos distintos. Ese fallo se midio y dio que un factor de 6 puntos
         * movia 54 y cambiaba el 77 % de las frases — imposible, y por eso se
         * vio. La comparacion exige que lo unico distinto entre las dos
         * poblaciones sea el vector.
         */
        boolean escribeBiografia = azar.nextInt(5) != 0;
        if (conBiografia && escribeBiografia) {
            // La semilla del vector va por persona: dos perfiles cercanos en el
            // indice se describen parecido, que es lo que hace que la afinidad
            // tenga algo que distinguir.
            VectorDePrueba.con(u, i * 0.37);
        }

        return new PerfilDeMatch(u, horarios(u, azar, horas), levantamientos(u, azar, reparto), constancia(azar));
    }

    /**
     * De cero a cuatro franjas, con horas que se solapan a medias entre unos y
     * otros.
     *
     * <p>La ventana va de las 7 a las 21 y las franjas duran dos horas: asi hay
     * parejas que coinciden entero, parejas que coinciden media hora y parejas
     * que no coinciden, que es el rango que el factor de horario tiene que saber
     * separar.
     */
    private static List<Disponibilidad> horarios(Usuario de, Random azar,
                                                RepartoDeHorarios horas) {
        // De una a cuatro, nunca cero: sin horario no se pasa del guardian.
        int cuantas = 1 + azar.nextInt(4);
        List<Disponibilidad> franjas = new ArrayList<>(cuantas);

        for (int i = 0; i < cuantas; i++) {
            // Los dos sorteos se hacen SIEMPRE y en el mismo orden. Lo unico que
            // cambia entre repartos es a que hora y a que dia se traducen, para
            // que la variante no desplace la secuencia de nadie.
            int sorteoHora = azar.nextInt(13);
            int sorteoDia = azar.nextInt(DIAS.length);

            int hora = horas == RepartoDeHorarios.HORAS_PUNTA
                    ? horaPunta(sorteoHora) : 7 + sorteoHora;
            int dia = horas == RepartoDeHorarios.HORAS_PUNTA
                    ? diaEntreSemana(sorteoDia) : sorteoDia;

            franjas.add(new Disponibilidad(
                    DIAS[dia],
                    LocalTime.of(hora, 0),
                    LocalTime.of(Math.min(hora + 2, 23), 0),
                    de,
                    // Una de cada cuatro es fija. Son las que valen doble en el
                    // solape, asi que si no hubiera ninguna el factor mas
                    // importante se mediria solo por su rama floja.
                    azar.nextInt(4) == 0));
        }
        return franjas;
    }

    /**
     * Los trece valores del sorteo, apilados donde la gente entrena de verdad.
     *
     * <p>Copiado del reparto de la demostracion: dos tercios por la tarde, un
     * cuarto a primera hora y algo suelto a mediodia. Las proporciones son de
     * esas 31 franjas escritas a mano, no de ninguna estadistica.
     */
    private static int horaPunta(int sorteo) {
        if (sorteo <= 1) return 7;      // 15 % primera hora
        if (sorteo == 2) return 8;      //  8 %
        if (sorteo == 3) return 13;     //  8 % mediodia
        if (sorteo <= 8) return 18;     // 38 % la hora punta de verdad
        if (sorteo <= 11) return 19;    // 23 %
        return 20;                      //  8 %
    }

    /** Sabado y domingo caen en dias de diario, que es donde se apila la gente. */
    private static int diaEntreSemana(int sorteo) {
        return switch (sorteo) {
            case 5 -> 1;   // sabado  -> martes
            case 6 -> 3;   // domingo -> jueves
            default -> sorteo;
        };
    }

    private static List<Levantamiento> levantamientos(Usuario de, Random azar,
                                                      RepartoDeEjercicios reparto) {
        // Uno de cada cuatro no ha apuntado ninguna marca.
        if (azar.nextInt(4) == 0) return List.of();

        List<Levantamiento> marcas = new ArrayList<>();

        for (int i = 0; i < 1 + azar.nextInt(3); i++) {
            Levantamiento l = new Levantamiento();
            l.setUsuario(de);
            // Un solo sorteo, dos traducciones: es lo que mantiene alineadas las
            // dos poblaciones del experimento.
            l.setEjercicio(ejercicioDe(azar.nextInt(Ejercicio.values().length), reparto));
            l.setPeso(40.0 + azar.nextInt(120));
            l.setRepeticiones(1 + azar.nextInt(10));
            marcas.add(l);
        }
        return marcas;
    }

    /**
     * A que ejercicio corresponde un sorteo, segun el reparto.
     *
     * <p>Sugiriendo los basicos, cinco de las seis caras del dado caen en
     * sentadilla, banca o peso muerto. No es una prediccion de lo que haria la
     * gente: es el escenario que se quiere medir —"y si el formulario los
     * sugiriera"— con un sesgo fuerte pero no total, porque quien entrena otra
     * cosa la seguira apuntando.
     */
    private static Ejercicio ejercicioDe(int sorteo, RepartoDeEjercicios reparto) {
        Ejercicio[] todos = Ejercicio.values();
        if (reparto == RepartoDeEjercicios.COMO_HOY) return todos[sorteo];

        return switch (sorteo) {
            case 0, 3 -> Ejercicio.SENTADILLA;
            case 1, 4 -> Ejercicio.PRESS_BANCA;
            case 2 -> Ejercicio.PESO_MUERTO;
            default -> Ejercicio.PRESS_MILITAR;
        };
    }

    /** Desde quien no ha registrado nada hasta quien aparece tres veces por semana. */
    private static Constancia constancia(Random azar) {
        if (azar.nextInt(6) == 0) return Constancia.DESCONOCIDA;
        return new Constancia(azar.nextInt(14), true);
    }
}
