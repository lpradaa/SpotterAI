package com.spotterai.backend.matching;

import com.spotterai.backend.textos.Mensaje;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Los levantamientos que se pueden comparar entre dos personas.
 *
 * <p>Lista cerrada y no texto libre: la gracia de este dato es que el motor
 * pueda cruzarlo, y "banca", "press banca" y "press de banca" escritos a mano
 * son tres ejercicios distintos para una maquina.
 *
 * <p>Solo movimientos con barra donde el peso es directamente comparable. Las
 * dominadas o los fondos se hacen con el peso corporal mas un lastre opcional,
 * asi que "15 kg" en dominadas y "15 kg" en sentadilla no significan nada
 * parecido y meterlos aqui ensuciaria la comparacion. Para eso estan las marcas
 * en texto libre del perfil.
 */
public enum Ejercicio {

    SENTADILLA(true),
    PRESS_BANCA(true),
    PESO_MUERTO(true),
    PRESS_MILITAR(false),
    REMO_BARRA(false),
    HIP_THRUST(false);

    /**
     * Si es uno de los tres que casi todo el mundo hace.
     *
     * <p>No es una jerarquia de ejercicios: es lo que hace que el factor de
     * fuerza tenga con que comparar. Comparar exige que <b>los dos</b> hayan
     * apuntado el mismo ejercicio, y con seis a elegir eso pasa poco: medido
     * sobre 1.770 parejas, solo el 22,5 % tenia alguno en comun, y por eso el
     * factor rendia la mitad que los demas (ver {@code docs/medir-el-motor.md}).
     *
     * <p>De las dos formas de arreglarlo, esta es la que no cambia lo que
     * significa el numero: sugerir los basicos sube la cobertura al 30,5 % y se
     * siguen comparando maximos del mismo ejercicio. La otra —comparar
     * ejercicios distintos del mismo patron— subia mas, hasta el 35,2 %, pero
     * cambiaba el veredicto en una de cada tres parejas donde se puede saber la
     * verdad: un peso muerto y una sentadilla no son el mismo numero.
     */
    private final boolean basico;

    Ejercicio(boolean basico) {
        this.basico = basico;
    }

    public boolean esBasico() {
        return basico;
    }

    /**
     * Los tres basicos primero, y el resto detras.
     *
     * <p>El orden es la sugerencia: no se quita ninguno ni se obliga a nada,
     * porque quien entrene otra cosa la tiene que poder apuntar igual. Lo unico
     * que cambia es cual esta delante cuando alguien abre el desplegable.
     */
    public static java.util.List<Ejercicio> sugeridosPrimero() {
        return java.util.Arrays.stream(values())
                .sorted(java.util.Comparator.comparing(Ejercicio::esBasico).reversed())
                .toList();
    }

    /**
     * El nombre del ejercicio, sin redactar.
     *
     * <p>Sale del catalogo y no de un texto escrito dentro del enum, por lo
     * mismo que {@link Rutina#nombre()}: este nombre viaja al frontend y se
     * pinta en la tabla de marcas de la ficha de una persona, asi que escrito
     * aqui salia «Press de banca» en medio de una pagina en ingles.
     */
    public Mensaje nombre() {
        return Mensaje.de("ejercicio." + name());
    }

    /** Vacio si el valor no es uno de los conocidos, en vez de reventar. */
    public static Optional<Ejercicio> desde(String valor) {
        if (valor == null || valor.isBlank()) return Optional.empty();
        String clave = valor.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(e -> e.name().equals(clave)).findFirst();
    }
}
