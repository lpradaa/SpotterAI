package com.spotterai.backend.matching;

/**
 * Con que regularidad entrena alguien de verdad.
 *
 * <p>Es el unico factor del motor que no sale de lo que alguien dice de si
 * mismo, sino de lo que ha hecho. Y es el que responde a la pregunta que de
 * verdad importa: de todos los que encajan contigo sobre el papel, ¿quien va a
 * aparecer el martes?
 *
 * <p>Puede encajar contigo al noventa por ciento —mismo gimnasio, mismas horas,
 * misma fuerza— alguien que lleva mes y medio sin pisar el gimnasio. Ese no es
 * un buen companero de entrenamiento: es un buen companero de entrenamiento
 * hipotetico.
 *
 * <p><b>Sin historial no se juzga.</b> Quien no ha registrado nunca nada puede
 * ser alguien que acaba de entrar, y castigar a los recien llegados con el
 * mismo rasero que a quien lo dejo hace dos meses seria confundir "no se sabe"
 * con "no va". Se trata como cualquier otro dato ausente: el factor sale del
 * calculo.
 *
 * @param entrenosRecientes sesiones registradas en la ventana que se mira
 * @param tieneHistorial    si esta persona ha registrado algo alguna vez
 */
public record Constancia(int entrenosRecientes, boolean tieneHistorial) {

    /** Ventana que se mira. Cuatro semanas: suficiente para ver un habito. */
    public static final int DIAS = 28;

    /**
     * Entrenamientos por semana a partir de los cuales la constancia es de sobra.
     *
     * <p>Tres. No cinco: el motor no premia a quien mas entrena, sino a quien
     * aparece con regularidad, que es lo que hace falta para quedar con alguien.
     */
    private static final double POR_SEMANA_IDEAL = 3.0;

    private static final double SEMANAS = DIAS / 7.0;

    /** Nadie ha registrado nada: no se sabe. */
    public static final Constancia DESCONOCIDA = new Constancia(0, false);

    /** De 0 a 1. Solo tiene sentido si {@link #tieneHistorial()}. */
    public double ritmo() {
        return Math.min(1.0, (entrenosRecientes / SEMANAS) / POR_SEMANA_IDEAL);
    }

    /**
     * Lo que vale la pareja, que es lo que vale el menos constante de los dos.
     *
     * <p>Una pareja entrena tan a menudo como el que menos aparece: de nada
     * sirve que tu vayas cuatro dias si el otro va uno.
     */
    public static double deLaPareja(Constancia mia, Constancia suya) {
        return Math.min(mia.ritmo(), suya.ritmo());
    }

    /** Frase para la explicacion, dicha del otro y no de la pareja. */
    public static String describir(Constancia mia, Constancia suya) {
        double ritmo = deLaPareja(mia, suya);

        if (ritmo >= 0.9) return "Los dos entrenáis con mucha regularidad";
        if (ritmo >= 0.6) return "Los dos entrenáis de forma bastante constante";
        if (ritmo >= 0.3) return "Alguno de los dos entrena de forma irregular";
        return "Alguno de los dos lleva tiempo sin entrenar";
    }
}
