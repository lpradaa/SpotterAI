package com.spotterai.backend.matching;

import com.spotterai.backend.semantica.VectorDeTexto;
import com.spotterai.backend.textos.Mensaje;

import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Levantamiento;
import com.spotterai.backend.models.Usuario;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
 *   Objetivo   15  que busca cada uno
 *   Gimnasio   15  condicion practica
 *   Nivel      10  el ritmo, tal y como lo declara cada uno
 *   Fuerza     10  si podeis cubriros con la barra cargada
 *   Rutina      5  si hareis lo mismo el mismo dia
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
    //
    // Ya no son `final`. Antes ajustar un peso exigia recompilar y desplegar;
    // ahora ConfiguracionDelMotor los rellena una vez al arrancar, desde
    // configuracion, con {@link #configurar}. Los valores de aqui abajo siguen
    // siendo los que rigen si nadie llama a configurar —en un test que no
    // levanta Spring, por ejemplo—, asi que el comportamiento por defecto no
    // cambia en nada.
    static double PESO_HORARIO = 40;
    /*
     * Nivel baja de 20 a 10 y esos 10 pasan a fuerza.
     *
     * Los dos miden lo mismo, pero uno es una etiqueta que se elige y el otro un
     * hecho: "Intermedio" significa cosas distintas para cada persona, y 100 kg
     * en banca no. Se le deja la mitad porque sigue sirviendo para quien no ha
     * registrado ningun levantamiento, que sera la mayoria al principio.
     */
    static double PESO_NIVEL = 10;
    static double PESO_FUERZA = 10;
    /*
     * Objetivo baja de 20 a 15 y esos 5 pasan a la rutina.
     *
     * Se solapan: quien busca fuerza y quien busca hipertrofia suelen repartir la
     * semana de forma parecida. Pero el objetivo dice que quieres y la rutina
     * dice que haces el martes, que es lo que decide si podeis compartir sesion.
     */
    static double PESO_OBJETIVO = 12;
    static double PESO_RUTINA = 5;
    /*
     * Gimnasio baja de 15 a 8.
     *
     * No porque importe menos —importa mas que nunca— sino porque ahora cuenta
     * dos veces: sigue sumando lo suyo y ademas condiciona el factor horario,
     * que es donde de verdad se nota. Dejarlo en 15 seria cobrar dos veces por
     * el mismo dato y hundir de mas a quien entrena en otro sitio.
     */
    static double PESO_GIMNASIO = 8;
    static double PESO_EDAD = 5;

    /*
     * Constancia: 10 puntos, de los 7 que suelta el gimnasio y 3 del objetivo.
     *
     * Es el unico factor que no sale de lo que alguien dice de si mismo sino de
     * lo que ha hecho, y responde a la pregunta que decide si el match sirve de
     * algo: de todos los que encajan sobre el papel, quien va a aparecer.
     */
    static double PESO_CONSTANCIA = 10;

    /*
     * Afinidad de lo que cada uno escribe sobre si mismo: 6 puntos.
     *
     * Los otros ocho factores puntuan campos de un formulario. Este es el unico
     * que mira lo unico que una persona escribe con sus palabras, y ahi hay
     * cosas que ninguna casilla recoge: "todavia me da respeto la zona de peso
     * libre" no es un nivel, "me amoldo a lo que haga falta" no es una rutina, y
     * "necesito a alguien que pueda ayudarme en banca pesada" es una peticion
     * explicita que no cabe en un desplegable.
     *
     * Seis puntos y no mas, por dos razones. Es una señal blanda —un modelo
     * midiendo parecido entre dos frases cortas— y no puede pesar como el
     * horario, que es una restriccion dura: si no coincidis, no entrenais
     * juntos, y ningun texto arregla eso. Y seis puntos bastan para mover una
     * decision en el margen: es lo que separa un 68 de un 74, que es la
     * frontera entre "buena compatibilidad" y "muy compatibles".
     *
     * Los pesos no tienen que sumar 100 —PesosDelMotor lo dice y la
     * redistribucion reescala en proporcion— asi que este factor no le quita
     * peso a nadie: cambia el reparto relativo, que es exactamente lo que debe
     * hacer un factor nuevo.
     */
    static double PESO_AFINIDAD = 6;

    /*
     * El suelo y el techo de la afinidad, MEDIDOS y no supuestos.
     *
     * Dos textos cualesquiera del mismo dominio —gente hablando de entrenar— ya
     * se parecen solo por el vocabulario compartido. Sin un suelo, el factor
     * daria medio punto a todo el mundo. Lo que puntua es cuanto se parecen por
     * encima de ese fondo comun.
     *
     * La primera version llevaba 0,30 y 0,75, elegidos a ojo. Al pasar las trece
     * biografias reales por el modelo, el rango observado entre las 21 parejas
     * resulto ser 0,085 - 0,544:
     *
     *     0,544  Alex + Marta      (los dos: constancia, acompanado, basicos)
     *     0,493  Diego + Noa
     *     0,472  Javi + Alex       (los dos vienen de fuerza)
     *     0,226  Javi + Lucia      (powerlifter contra principiante)
     *     0,085  Javi + Diego      (el mas bajo)
     *
     * Con 0,30-0,75 la mejor pareja real se habria quedado en la mitad del
     * factor y la mayoria en cero: un factor practicamente inerte. Con
     * 0,15-0,55 el reparto usa el rango entero. Estos numeros son de este modelo
     * y de este dominio; cambiar de modelo obliga a volver a medirlos.
     */
    private static final double AFINIDAD_MINIMA = 0.15;
    private static final double AFINIDAD_DE_SOBRA = 0.55;

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
     * Lo que vale coincidir en horario cuando entrenais en gimnasios distintos.
     *
     * <p>Casi nada, y por una razon que no es de matiz: <b>no coincidis</b>. Tu a
     * las seis en McFit y ella a las seis en Basic-Fit no estais juntos, estais
     * en dos edificios de la ciudad a la misma hora. La aplicacion llegaba a
     * decir "los dos vais siempre un dia a la misma hora (Lunes)" de una pareja
     * asi, que es literalmente falso en lo unico que importa.
     *
     * <p>Es el mismo error que ya se corrigio un nivel mas abajo con la rutina
     * —coincidir con quien ese dia hace pierna mientras tu haces pecho es
     * coincidir en el gimnasio, no entrenar juntos— aplicado un nivel mas
     * arriba.
     *
     * <p>No se anula del todo porque la informacion sigue valiendo para algo: si
     * los dos podeis los lunes a las seis, resuelto el gimnasio entrenariais
     * juntos. Pero resolver el gimnasio significa que uno de los dos se cambie o
     * pague una entrada, y la mayoria de las parejas no lo hacen.
     */
    static final double SOLAPE_EN_OTRO_GIMNASIO = 0.25;

    /**
     * Lo mismo, cuando alguno de los dos ha dicho que se desplazaria.
     *
     * <p>"La mayoria de las parejas no lo hacen" seguia siendo verdad, pero era
     * una media aplicada a todo el mundo por igual: quien esta dispuesto a coger
     * el metro tres paradas puntuaba exactamente igual que quien no piensa
     * moverse, y la aplicacion no tenia forma de saber la diferencia porque
     * nunca lo preguntaba.
     *
     * <p><b>Basta con que lo diga uno.</b> Para que la pareja funcione solo hace
     * falta que se mueva una persona, y exigirlo a los dos dejaria fuera al caso
     * mas normal: uno con el gimnasio al lado de casa y otro dispuesto a ir.
     *
     * <p>Muy por encima de 0,25 pero claramente por debajo de 1: desplazarse
     * sigue costando tiempo y a menudo una entrada, asi que una pareja de
     * gimnasios distintos no puede empatar nunca con una que comparte sala y
     * tiene el mismo solape. Y el factor gimnasio sigue dando cero: no comparten
     * gimnasio, eso no lo cambia estar dispuesto a viajar. Lo que cambia es lo
     * que significa coincidir en horario, que es exactamente lo que este numero
     * pondera.
     */
    static final double SOLAPE_CON_DESPLAZAMIENTO = 0.60;

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

    /**
     * Cambia el reparto de pesos entre los ocho factores.
     *
     * <p>Pensado para llamarse una vez, al arrancar, desde
     * {@code ConfiguracionDelMotor}. No es {@code synchronized} ni thread-safe
     * frente a llamadas concurrentes: el reparto de pesos no cambia mientras la
     * aplicacion esta sirviendo trafico, cambia entre despliegues, y añadir
     * bloqueo aqui seria proteger un caso que no existe.
     *
     * <p><b>Ojo si se llama desde una prueba.</b> Estos campos son estaticos y
     * por tanto compartidos por toda la maquina virtual: una prueba que llame a
     * este metodo con valores distintos de los de fabrica y no los devuelva a
     * su sitio deja el resto de la suite —incluidas pruebas de otras clases,
     * si se ejecutan en el mismo proceso— calculando con un motor distinto del
     * que creen estar probando. Quien lo haga, que restaure los valores en un
     * {@code @AfterEach} o equivalente.
     */
    public static void configurar(PesosDelMotor pesos) {
        PESO_HORARIO = pesos.horario();
        PESO_NIVEL = pesos.nivel();
        PESO_FUERZA = pesos.fuerza();
        PESO_OBJETIVO = pesos.objetivo();
        PESO_CONSTANCIA = pesos.constancia();
        PESO_RUTINA = pesos.rutina();
        PESO_GIMNASIO = pesos.gimnasio();
        PESO_EDAD = pesos.edad();
    }

    /**
     * La unica forma de puntuar una pareja.
     *
     * <p>Aqui hubo dos sobrecargas mas —una sin levantamientos y otra sin
     * constancia— pensadas para quien no tuviera esos datos. La idea era comoda
     * y el efecto fue el contrario: llamar a la corta no da error, da otro
     * numero. Paso tres veces. El tablero y Explorar ensenaban 90 y 83 de la
     * misma pareja; despues la lista decia 56 y la explicacion 48.
     *
     * <p>Las dos veces se arreglo la llamada y se dejo el atajo con un aviso en
     * un comentario. Un aviso en un comentario no es una defensa: a la tercera
     * se quita el atajo.
     *
     * <p>Quien de verdad no tenga un dato construye el {@link PerfilDeMatch}
     * diciendolo —{@code PerfilDeMatch.de(usuario, horarios)}— y entonces la
     * ausencia esta escrita en la llamada, no escondida en que sobrecarga
     * resuelve el compilador.
     */
    public static PuntuacionCompatibilidad calcular(PerfilDeMatch mio, PerfilDeMatch suyo) {
        Usuario yo = mio.usuario();
        Usuario otro = suyo.usuario();

        SolapeHorario solape = CalculadoraSolape.calcular(mio.horarios(), suyo.horarios());

        List<FactorCompatibilidad> brutos = List.of(
                factorHorario(solape, mio.horarios(), suyo.horarios(), gimnasiosDe(yo, otro)),
                factorNivel(yo.getNivel(), otro.getNivel()),
                factorFuerza(mio.levantamientos(), suyo.levantamientos()),
                factorObjetivo(yo.getObjetivos(), otro.getObjetivos()),
                factorConstancia(mio.constancia(), suyo.constancia()),
                factorRutina(yo.getRutina(), otro.getRutina()),
                factorGimnasio(yo, otro),
                factorEdad(yo.getEdad(), otro.getEdad()),
                factorAfinidad(yo, otro));

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
        // Los NUEVE pesos. Esta suma se escribio a mano cuando eran ocho, y al
        // entrar la afinidad se quedo corta: el peso evaluado incluia los 6 del
        // factor nuevo y el total no, asi que el cociente pasaba de 1 y la
        // confianza multiplicaba por mas de uno. Puntuaciones infladas, sin
        // error y sin nada que lo delatara salvo numeros que ya no cuadraban.
        //
        // Enumerar pesos a mano es la clase de sitio donde esto vuelve a pasar
        // al decimo factor.
        double pesoTotal = PESO_HORARIO + PESO_NIVEL + PESO_FUERZA + PESO_OBJETIVO
                + PESO_CONSTANCIA + PESO_RUTINA + PESO_GIMNASIO + PESO_EDAD + PESO_AFINIDAD;
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

    /**
     * En que relacion estan los dos gimnasios.
     *
     * <p>Hace falta en el factor horario y no solo en el suyo propio: el
     * gimnasio no es un merito que suma aparte, es la condicion bajo la cual
     * coincidir en horario significa algo.
     */
    private enum Gimnasios {
        MISMO,
        DISTINTOS,
        /** Distintos, pero al menos uno de los dos ha dicho que se desplazaria. */
        DISTINTOS_PERO_ALGUIEN_SE_MUEVE,
        SIN_SABER
    }

    private static Gimnasios gimnasiosDe(Usuario yo, Usuario otro) {
        Long mio = yo.getGimnasio() != null ? yo.getGimnasio().getId() : null;
        Long suyo = otro.getGimnasio() != null ? otro.getGimnasio().getId() : null;

        if (mio == null || suyo == null) return Gimnasios.SIN_SABER;
        if (mio.equals(suyo)) return Gimnasios.MISMO;

        // Uno basta: para que la pareja funcione solo hace falta que se mueva
        // una persona, no las dos.
        boolean algunoSeMueve = yo.isPuedoDesplazarme() || otro.isPuedoDesplazarme();
        return algunoSeMueve ? Gimnasios.DISTINTOS_PERO_ALGUIEN_SE_MUEVE : Gimnasios.DISTINTOS;
    }

    private static FactorCompatibilidad factorHorario(
            SolapeHorario solape, List<Disponibilidad> mios, List<Disponibilidad> suyos,
            Gimnasios gimnasios) {

        // Sin horarios en alguno de los dos perfiles no es que no coincidais: es que
        // no lo sabemos. El factor sale del calculo en vez de restar 40 puntos.
        boolean faltanDatos = mios == null || mios.isEmpty() || suyos == null || suyos.isEmpty();
        if (faltanDatos) {
            return FactorCompatibilidad.sinDatos("horario",
                    Mensaje.de("factor.horario.sinDatos"));
        }

        if (!solape.hayCoincidencia()) {
            return FactorCompatibilidad.evaluado("horario", 0, PESO_HORARIO,
                    Mensaje.de("factor.horario.sinSolape"));
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

        // El gimnasio, aqui y no solo en su propio factor. La penalizacion se
        // aplica al final, despues del suelo por anclas: ir los dos siempre al
        // mismo hora no es un ancla si es a sitios distintos.
        if (gimnasios == Gimnasios.DISTINTOS) {
            ratio *= SOLAPE_EN_OTRO_GIMNASIO;
        } else if (gimnasios == Gimnasios.DISTINTOS_PERO_ALGUIEN_SE_MUEVE) {
            ratio *= SOLAPE_CON_DESPLAZAMIENTO;
        }

        return FactorCompatibilidad.evaluado("horario", ratio * PESO_HORARIO, PESO_HORARIO,
                describirSolape(solape, gimnasios));
    }

    private static double sueloPorAnclas(int diasAncla) {
        if (diasAncla >= 2) return SUELO_VARIAS_ANCLAS;
        if (diasAncla == 1) return SUELO_UNA_ANCLA;
        return 0;
    }

    private static Mensaje describirSolape(SolapeHorario solape, Gimnasios gimnasios) {
        // Lo primero que hay que decir cuando los gimnasios no son el mismo,
        // porque cambia el significado de todo lo demas.
        if (gimnasios == Gimnasios.DISTINTOS) {
            return Mensaje.de("factor.horario.gimnasiosDistintos",
                    duracion(solape.minutosSemanales()));
        }

        // La misma frase, pero con la salida: el dato que la cambia lo ha puesto
        // una persona, no lo ha deducido nadie.
        if (gimnasios == Gimnasios.DISTINTOS_PERO_ALGUIEN_SE_MUEVE) {
            return Mensaje.de("factor.horario.alguienSeMueve",
                    duracion(solape.minutosSemanales()));
        }

        if (solape.hayAncla()) {
            Mensaje dias = solape.diasAncla() == 1
                    ? Mensaje.de("comun.dias.uno")
                    : Mensaje.de("comun.dias.varios", solape.diasAncla());
            // Los dias de ancla, no todos: enumerar aqui solape.dias() hacia que
            // la frase se contradijera sola ("vais siempre 2 dias" y detras tres
            // dias entre parentesis).
            return Mensaje.de("factor.horario.ambosFijos", dias, enumerar(solape.diasDeAncla()));
        }
        return Mensaje.de("factor.horario.solape",
                duracion(solape.minutosSemanales()), enumerar(solape.dias()));
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
                    Mensaje.de("factor.fuerza.sinDatos"));
        }

        return FactorCompatibilidad.evaluado("fuerza",
                comparacion.ratio() * PESO_FUERZA, PESO_FUERZA,
                CalculadoraFuerza.describir(comparacion));
    }

    /**
     * Si vais a aparecer.
     *
     * <p>El unico factor que no sale de lo que alguien dice de si mismo. Alguien
     * puede encajar contigo al noventa por ciento y llevar mes y medio sin pisar
     * el gimnasio: eso no es un buen companero, es un buen companero hipotetico.
     *
     * <p>Sin historial no se juzga, que es la diferencia entre alguien que acaba
     * de entrar y alguien que lo dejo.
     */
    private static FactorCompatibilidad factorConstancia(Constancia mia, Constancia suya) {
        if (!mia.tieneHistorial() || !suya.tieneHistorial()) {
            return FactorCompatibilidad.sinDatos("constancia",
                    Mensaje.de("factor.constancia.sinDatos"));
        }

        double ritmo = Constancia.deLaPareja(mia, suya);
        return FactorCompatibilidad.evaluado("constancia",
                ritmo * PESO_CONSTANCIA, PESO_CONSTANCIA,
                Constancia.describir(mia, suya));
    }

    /**
     * Si vais a estar haciendo lo mismo el mismo dia.
     *
     * El objetivo dice que buscas; la rutina dice que tocas el martes. Coincidir
     * en horario con alguien que ese dia hace pierna cuando tu haces pecho es
     * coincidir en el gimnasio, no entrenar juntos.
     */
    private static FactorCompatibilidad factorRutina(String miRutina, String suRutina) {
        Optional<Rutina> mia = Rutina.desde(miRutina);
        Optional<Rutina> suya = Rutina.desde(suRutina);

        if (mia.isEmpty() || suya.isEmpty()) {
            return FactorCompatibilidad.sinDatos("rutina",
                    Mensaje.de("factor.rutina.sinDatos"));
        }

        double afinidad = Rutina.afinidad(mia.get(), suya.get());
        return FactorCompatibilidad.evaluado("rutina",
                afinidad * PESO_RUTINA, PESO_RUTINA,
                Rutina.describir(mia.get(), suya.get()));
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
                    Mensaje.de("factor.nivel.sinDatos"));
        }

        int distancia = Math.abs(a - b);
        return switch (distancia) {
            case 0 -> FactorCompatibilidad.evaluado("nivel", PESO_NIVEL, PESO_NIVEL,
                    Mensaje.de("factor.nivel.igual", nombreDeNivel(claveB)));
            case 1 -> FactorCompatibilidad.evaluado("nivel", PESO_NIVEL / 2, PESO_NIVEL,
                    Mensaje.de("factor.nivel.contiguos",
                            nombreDeNivel(claveA), nombreDeNivel(claveB)));
            default -> FactorCompatibilidad.evaluado("nivel", 0, PESO_NIVEL,
                    Mensaje.de("factor.nivel.lejanos",
                            nombreDeNivel(claveA), nombreDeNivel(claveB)));
        };
    }

    private static FactorCompatibilidad factorObjetivo(String miObjetivo, String suObjetivo) {
        String a = normalizar(miObjetivo);
        String b = normalizar(suObjetivo);

        if (a == null || b == null) {
            return FactorCompatibilidad.sinDatos("objetivo",
                    Mensaje.de("factor.objetivo.sinDatos"));
        }
        if (a.equals(b)) {
            return FactorCompatibilidad.evaluado("objetivo", PESO_OBJETIVO, PESO_OBJETIVO,
                    Mensaje.de("factor.objetivo.igual", nombreDeObjetivo(b)));
        }
        boolean afines = OBJETIVOS_AFINES.stream().anyMatch(g -> g.contains(a) && g.contains(b));
        if (afines) {
            return FactorCompatibilidad.evaluado("objetivo", PESO_OBJETIVO / 2, PESO_OBJETIVO,
                    Mensaje.de("factor.objetivo.compatibles",
                            nombreDeObjetivo(a), nombreDeObjetivo(b)));
        }
        return FactorCompatibilidad.evaluado("objetivo", 0, PESO_OBJETIVO,
                Mensaje.de("factor.objetivo.distintos",
                        nombreDeObjetivo(a), nombreDeObjetivo(b)));
    }

    private static FactorCompatibilidad factorGimnasio(Usuario yo, Usuario otro) {
        Long mio = yo.getGimnasio() != null ? yo.getGimnasio().getId() : null;
        Long suyo = otro.getGimnasio() != null ? otro.getGimnasio().getId() : null;

        if (mio == null || suyo == null) {
            return FactorCompatibilidad.sinDatos("gimnasio",
                    Mensaje.de("factor.gimnasio.sinDatos"));
        }
        if (mio.equals(suyo)) {
            return FactorCompatibilidad.evaluado("gimnasio", PESO_GIMNASIO, PESO_GIMNASIO,
                    Mensaje.de("factor.gimnasio.mismo", otro.getGimnasio().getNombre()));
        }
        // Sigue siendo cero aunque alguien se desplace: no comparten gimnasio, y
        // estar dispuesto a viajar no cambia ese hecho. Lo que cambia es lo que
        // significa coincidir en horario, y eso se pondera en el factor horario.
        boolean algunoSeMueve = yo.isPuedoDesplazarme() || otro.isPuedoDesplazarme();
        return FactorCompatibilidad.evaluado("gimnasio", 0, PESO_GIMNASIO,
                Mensaje.de(algunoSeMueve
                        ? "factor.gimnasio.distintosSeMueve"
                        : "factor.gimnasio.distintos"));
    }

    private static FactorCompatibilidad factorEdad(Integer miEdad, Integer suEdad) {
        if (miEdad == null || suEdad == null) {
            return FactorCompatibilidad.sinDatos("edad",
                    Mensaje.de("factor.edad.sinDatos"));
        }

        int diferencia = Math.abs(miEdad - suEdad);
        double puntos;
        if (diferencia <= 3) puntos = PESO_EDAD;
        else if (diferencia <= 7) puntos = PESO_EDAD * 0.6;
        else if (diferencia <= 12) puntos = PESO_EDAD * 0.2;
        else puntos = 0;

        Mensaje detalle = diferencia <= 3
                ? Mensaje.de("factor.edad.misma")
                : Mensaje.de("factor.edad.diferencia", diferencia);
        return FactorCompatibilidad.evaluado("edad", puntos, PESO_EDAD, detalle);
    }

    /**
     * Cuanto se parece lo que cada uno ha escrito sobre si mismo.
     *
     * <p>El unico factor que no sale de un desplegable. Compara los vectores de
     * las dos biografias — que se calcularon al guardar cada perfil, no aqui:
     * esto es un producto escalar sobre datos ya en memoria y no cuesta ni una
     * llamada de red, que es lo que permite añadir un noveno factor sin tocar
     * los 44 ms de la consulta.
     *
     * <p><b>El modelo no decide el orden.</b> Aporta una señal que el motor
     * pondera junto a las otras ocho, igual que cualquier otra. Una puntuacion
     * que ordena a la gente tiene que ser instantanea, identica entre
     * ejecuciones y explicable; un modelo puntuando da un producto que no se
     * puede depurar ni defender.
     *
     * <p>Sin vector en alguno de los dos no es incompatibilidad, es falta de
     * datos: alguien que no ha escrito biografia no debe salir penalizado frente
     * a quien si, del mismo modo que no rellenar el gimnasio no significa
     * entrenar en otro sitio.
     */
    /**
     * Lo que cada uno ha escrito sobre como quiere entrenar: 6 puntos.
     *
     * <h2>Por que ya no es un coseno</h2>
     *
     * <p>Hasta la V19 esto comparaba los vectores de las dos biografias. Medido,
     * aquello ordenaba por <b>parecido de redaccion</b> y no por compatibilidad:
     * dos personas que querian lo contrario dicho con la misma estructura
     * sacaban 0,843 y dos que querian lo mismo dicho con sus palabras, 0,499. Y
     * no era cosa del modelo —otro de la misma clase salia peor— sino de que un
     * bi-encoder proyecta cada texto por separado y la oposicion entre dos
     * frases no es propiedad de ninguna de las dos.
     *
     * <p>Ahora cada biografia se lee por separado en tres ejes —que busca del
     * otro, cuanta ambicion, cuanta flexibilidad— y aqui solo se restan
     * posiciones. Todo el recorrido esta en {@code docs/medir-el-motor.md}.
     *
     * <h2>Eje a eje, y solo los que los dos han dicho</h2>
     *
     * <p>Un eje del que uno de los dos no habla <b>no se evalua</b>. La mitad de
     * las biografias reales no dicen nada de la mitad de los ejes, y colocar en
     * el centro a quien calla seria atribuirle una postura que no ha dado. Si no
     * queda ningun eje comun, el factor entero se declara sin datos y sus puntos
     * se reparten entre los demas, igual que cuando falta el gimnasio.
     */
    private static FactorCompatibilidad factorAfinidad(Usuario yo, Usuario otro) {
        double[] mias = {
                nulo(yo.getIntencionExigencia()), nulo(yo.getIntencionAmbicion()),
                nulo(yo.getIntencionFlexibilidad())};
        double[] suyas = {
                nulo(otro.getIntencionExigencia()), nulo(otro.getIntencionAmbicion()),
                nulo(otro.getIntencionFlexibilidad())};

        double suma = 0;
        int comunes = 0;
        int masParecido = -1;
        double mejorEncaje = -1;

        for (int i = 0; i < mias.length; i++) {
            if (Double.isNaN(mias[i]) || Double.isNaN(suyas[i])) continue;

            // Las posiciones van de -1 a 1, asi que la distancia maxima es 2.
            double encaje = 1.0 - Math.abs(mias[i] - suyas[i]) / 2.0;
            suma += encaje;
            comunes++;

            if (encaje > mejorEncaje) {
                mejorEncaje = encaje;
                masParecido = i;
            }
        }

        if (comunes == 0) {
            return FactorCompatibilidad.sinDatos("afinidad",
                    Mensaje.de("factor.afinidad.sinDatos"));
        }

        double ratio = Math.clamp(suma / comunes, 0.0, 1.0);

        // La frase dice EN QUE coincidis, no cuanto. Es la diferencia entre un
        // factor que puntua y uno que se explica, y era imposible con 384
        // numeros: no habia nada que nombrar.
        Mensaje detalle = ratio >= 0.75
                ? Mensaje.de("factor.afinidad.coincidis", Mensaje.de(CLAVES_DE_EJE[masParecido]))
                : ratio >= 0.4 ? Mensaje.de("factor.afinidad.algo")
                               : Mensaje.de("factor.afinidad.poca");

        return FactorCompatibilidad.evaluado("afinidad", ratio * PESO_AFINIDAD, PESO_AFINIDAD, detalle);
    }

    /** Un eje del que alguien no ha hablado. NaN porque no es un valor, es un hueco. */
    private static double nulo(Double valor) {
        return valor == null ? Double.NaN : valor;
    }

    /** En el mismo orden en el que se leen los ejes de la entidad. */
    private static final String[] CLAVES_DE_EJE = {
            "factor.afinidad.eje.exigencia", "factor.afinidad.eje.ambicion",
            "factor.afinidad.eje.flexibilidad"};

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Una duracion, sin redactar.
     *
     * <p>Devolvia una cadena —"2 horas", "1h 30min"— y esa cadena se metia dentro
     * de otra frase. Traducir solo la frase de fuera dejaba "You would overlap
     * 2 horas a week", que es el fallo tipico al traducir texto compuesto.
     */
    static Mensaje duracion(int minutos) {
        int horas = minutos / 60;
        int resto = minutos % 60;
        if (horas == 0) return Mensaje.de("duracion.minutos", resto);
        if (resto == 0) return horas == 1 ? Mensaje.de("duracion.hora") : Mensaje.de("duracion.horas", horas);
        return Mensaje.de("duracion.horasYMinutos", horas, resto);
    }

    /**
     * Los dias, enumerados y traducidos.
     *
     * <p>Se encadenan mensajes en vez de unir cadenas con ", " y " y ": la
     * conjuncion final tambien cambia de idioma, y aqui no sabemos en cual se va
     * a leer esto. Tres dias salen como coma(coma?no: coma(d1, d2) mas dos(.., d3)),
     * es decir "Lunes, Martes y Miercoles" y "Monday, Tuesday and Wednesday" con
     * la misma construccion.
     */
    static Mensaje enumerar(List<String> elementos) {
        if (elementos.isEmpty()) return Mensaje.de("lista.vacia");
        if (elementos.size() == 1) return nombreDeDia(elementos.get(0));

        Mensaje acumulado = nombreDeDia(elementos.get(0));
        for (int i = 1; i < elementos.size() - 1; i++) {
            acumulado = Mensaje.de("lista.coma", acumulado, nombreDeDia(elementos.get(i)));
        }
        return Mensaje.de("lista.dos", acumulado, nombreDeDia(elementos.get(elementos.size() - 1)));
    }

    /**
     * El nombre de un dia, que en la base esta guardado en español.
     *
     * <p>Los horarios se guardaron con "Lunes", "Miercoles"… de cuando no habia
     * mas que un idioma. Se normaliza a la clave del catalogo en vez de migrar la
     * columna: cambiar el dato guardado obligaria a tocar tambien todo lo que lo
     * compara, y lo que hace falta traducir es como se lee, no como se guarda.
     */
    private static Mensaje nombreDeDia(String dia) {
        String clave = sinAcentos(dia);
        return CLAVES_DE_DIA.contains(clave) ? Mensaje.de("dia." + clave) : Mensaje.de("lista.tal_cual", dia);
    }

    /** Igual que los dias: guardado en español, se normaliza para buscarlo. */
    private static Mensaje nombreDeNivel(String claveNormalizada) {
        return claveNormalizada != null && NIVELES_CONOCIDOS.contains(claveNormalizada)
                ? Mensaje.de("nivel." + claveNormalizada)
                : Mensaje.de("lista.tal_cual", claveNormalizada);
    }

    /** El espacio de "perdida de peso" pasa a guion bajo: en un .properties parte la clave. */
    private static Mensaje nombreDeObjetivo(String claveNormalizada) {
        if (claveNormalizada == null) return Mensaje.de("lista.tal_cual", "");
        String clave = claveNormalizada.replace(' ', '_');
        return OBJETIVOS_CONOCIDOS.contains(clave)
                ? Mensaje.de("objetivo." + clave)
                : Mensaje.de("lista.tal_cual", claveNormalizada);
    }

    private static final Set<String> CLAVES_DE_DIA = Set.of(
            "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO");

    private static final Set<String> NIVELES_CONOCIDOS =
            Set.of("principiante", "intermedio", "avanzado");

    private static final Set<String> OBJETIVOS_CONOCIDOS =
            Set.of("hipertrofia", "fuerza", "perdida_de_peso", "resistencia");

    /** "Miercoles" y "Miercoles" tienen que dar la misma clave. */
    private static String sinAcentos(String valor) {
        if (valor == null) return "";
        String descompuesto = java.text.Normalizer.normalize(valor.trim(), java.text.Normalizer.Form.NFD);
        return descompuesto.replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
    }
}
