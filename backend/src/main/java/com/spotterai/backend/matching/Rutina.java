package com.spotterai.backend.matching;

import com.spotterai.backend.textos.Mensaje;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Como reparte alguien los grupos musculares a lo largo de la semana.
 *
 * <p>Esto es lo que decide si dos personas pueden compartir sesion de verdad, y
 * no los ejercicios que les gusten: si los dos seguis empuje/tiron/pierna y
 * coincidis un martes, ese martes hareis el mismo bloque. Si tu haces
 * empuje/tiron/pierna y yo torso/pierna, coincidimos en horario pero cada uno va
 * a lo suyo.
 *
 * <p>{@link #LIBRE} no es "sin datos": es una respuesta. Quien no sigue una
 * division fija se adapta a lo que haga el otro, y eso encaja razonablemente con
 * cualquiera.
 */
public enum Rutina {

    FULL_BODY("Cuerpo completo", false),
    TORSO_PIERNA("Torso / Pierna", true),
    EMPUJE_TIRON_PIERNA("Empuje / Tirón / Pierna", true),
    WEIDER("Weider (un grupo por día)", true),
    LIBRE("Sin rutina fija", false);

    private final String nombre;

    /** Si reparte los grupos en dias distintos, en vez de tocarlo todo cada sesion. */
    private final boolean dividida;

    Rutina(String nombre, boolean dividida) {
        this.nombre = nombre;
        this.dividida = dividida;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean esDividida() {
        return dividida;
    }

    public static Optional<Rutina> desde(String valor) {
        if (valor == null || valor.isBlank()) return Optional.empty();
        String clave = valor.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(r -> r.name().equals(clave)).findFirst();
    }

    /**
     * Cuanto encajan dos formas de repartir la semana, de 0 a 1.
     *
     * <p>Por tramos, porque la diferencia que importa no es gradual: o hareis el
     * mismo bloque el mismo dia, o no.
     */
    public static double afinidad(Rutina mia, Rutina suya) {
        if (mia == suya) return 1.0;

        // Quien no sigue division se amolda a lo que toque ese dia. No es un
        // encaje perfecto —no comparte la progresion— pero tampoco un choque.
        if (mia == LIBRE || suya == LIBRE) return 0.6;

        // Dos divisiones distintas comparten bastantes dias: el de pierna cae en
        // los dos, y los empujes y tirones se solapan a medias.
        if (mia.esDividida() && suya.esDividida()) return 0.5;

        // Cuerpo completo contra una division: uno lo toca todo cada sesion y el
        // otro va por bloques, asi que casi nunca haran lo mismo a la vez.
        return 0.2;
    }

    /** Frase para la explicacion del match. */
    public static Mensaje describir(Rutina mia, Rutina suya) {
        if (mia == suya) {
            return mia == LIBRE
                    ? Mensaje.de("rutina.ambosLibres")
                    : Mensaje.de("rutina.misma", mia.nombre());
        }
        if (mia == LIBRE || suya == LIBRE) {
            Rutina laFija = mia == LIBRE ? suya : mia;
            return Mensaje.de("rutina.unoSeAdapta", laFija.nombre());
        }
        if (mia.esDividida() && suya.esDividida()) {
            return Mensaje.de("rutina.distintasPeroCoinciden", mia.nombre(), suya.nombre());
        }
        return Mensaje.de("rutina.dificiles", mia.nombre(), suya.nombre());
    }

    /**
     * El nombre de la rutina, sin redactar.
     *
     * <p>{@code getNombre()} sigue existiendo y sigue devolviendo el español: lo
     * usan el DTO del perfil y el sembrador. Lo que cambia es que las frases del
     * motor ya no lo meten dentro tal cual, porque entonces la frase se traducia
     * y la rutina de dentro no.
     */
    public Mensaje nombre() {
        return Mensaje.de("rutina." + name());
    }
}
