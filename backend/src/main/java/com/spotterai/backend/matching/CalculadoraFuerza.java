package com.spotterai.backend.matching;

import com.spotterai.backend.textos.Mensaje;

import com.spotterai.backend.models.Levantamiento;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Compara la fuerza de dos personas para saber si pueden entrenar juntas.
 *
 * <p>Esto no mide quien es mejor. Mide si la pareja funciona, que es otra cosa:
 * el producto se llama Spotter y un spotter que no puede con tu peso no te
 * sirve de nada. Si tu haces banca con 100 kg y yo muevo 45, no puedo sacarte
 * de debajo de la barra. Es seguridad, no preferencia.
 *
 * <p>La puntuacion es simetrica a proposito, aunque la utilidad real no lo sea
 * —el fuerte si puede asistir al flojo—. El motivo es que entrenar juntos de
 * verdad significa alternar en la misma barra: cuando levantais parecido
 * cambiais menos discos y os cubris mutuamente, y cuando hay mucha diferencia
 * uno se pasa la sesion esperando. Ademas, una nota que a ti te saliera 80 y a
 * la otra persona 40 seria imposible de explicar.
 */
public final class CalculadoraFuerza {

    private CalculadoraFuerza() {
    }

    /** A partir de aqui podeis alternar en la misma barra sin tocar los discos. */
    private static final double RATIO_MISMA_BARRA = 0.85;

    /** Os podeis asistir sin problema, cambiando peso entre series. */
    private static final double RATIO_ASISTENCIA_COMODA = 0.70;

    /** Uno puede cubrir al otro, pero no al reves. */
    private static final double RATIO_ASISTENCIA_LIMITE = 0.50;

    /**
     * Tope de repeticiones que se admiten al estimar.
     *
     * La formula de Epley se desvia bastante por encima de las doce, asi que
     * mas alla de ahi el numero estimado dejaria de significar nada.
     */
    public static final int MAX_REPETICIONES = 12;

    /** Resultado de la comparacion. Vacio si no hay ningun ejercicio en comun. */
    public record Comparacion(double ratio, List<Ejercicio> comunes) {

        public boolean hayDatos() {
            return !comunes.isEmpty();
        }

        /**
         * Si os podeis cubrir con la barra cargada sin que sea una odisea.
         *
         * <p>Es el mismo umbral que usa {@link #puntuar}, no una copia con otro
         * numero: la lista de Explorar filtra por esto y la explicacion del match
         * habla de lo mismo, asi que si dijeran cosas distintas una de las dos
         * estaria mintiendo.
         *
         * @return vacio cuando no hay ningun ejercicio en comun, que no es lo
         *         mismo que "no podeis": es que no se sabe
         */
        public Optional<Boolean> podeisCubriros() {
            if (!hayDatos()) return Optional.empty();
            return Optional.of(ratio >= RATIO_ASISTENCIA_COMODA);
        }
    }

    /**
     * Estimacion del maximo a una repeticion (Epley).
     *
     * Hace falta porque 100 kg a una repeticion y 100 kg a diez son fuerzas muy
     * distintas: sin llevarlos a una escala comun, comparar los pesos en bruto
     * daria por iguales a dos personas que no lo son.
     */
    public static double maximoEstimado(double peso, int repeticiones) {
        int reps = Math.max(1, Math.min(MAX_REPETICIONES, repeticiones));
        return peso * (1 + reps / 30.0);
    }

    public static Comparacion comparar(List<Levantamiento> mios, List<Levantamiento> suyos) {
        Map<Ejercicio, Double> porEjercicioMio = indexar(mios);
        Map<Ejercicio, Double> porEjercicioSuyo = indexar(suyos);

        double suma = 0;
        List<Ejercicio> comunes = new java.util.ArrayList<>();

        for (Map.Entry<Ejercicio, Double> entrada : porEjercicioMio.entrySet()) {
            Double suyo = porEjercicioSuyo.get(entrada.getKey());
            if (suyo == null) continue;

            double mio = entrada.getValue();
            // El menor entre el mayor: 1 es identico, 0,5 es que uno dobla al otro
            double ratio = Math.min(mio, suyo) / Math.max(mio, suyo);

            suma += puntuar(ratio);
            comunes.add(entrada.getKey());
        }

        // Sin ejercicios en comun no es que seais incompatibles, es que no se
        // puede saber: uno registro sentadilla y banca, el otro remo y militar.
        if (comunes.isEmpty()) return new Comparacion(0, List.of());

        return new Comparacion(suma / comunes.size(), comunes);
    }

    /**
     * Traduce la diferencia de fuerza a lo que significa en el gimnasio.
     *
     * Por tramos y no lineal porque el salto que importa no es proporcional:
     * entre levantar lo mismo y levantar un 15 % menos casi no hay diferencia
     * practica, y entre la mitad y un tercio tampoco, porque en los dos casos
     * ya no puedes cubrir al otro.
     */
    private static double puntuar(double ratio) {
        if (ratio >= RATIO_MISMA_BARRA) return 1.0;
        if (ratio >= RATIO_ASISTENCIA_COMODA) return 0.65;
        if (ratio >= RATIO_ASISTENCIA_LIMITE) return 0.30;
        return 0.05;
    }

    /** El maximo estimado de cada ejercicio, quedandose con el mejor si hay repetidos. */
    private static Map<Ejercicio, Double> indexar(List<Levantamiento> levantamientos) {
        Map<Ejercicio, Double> porEjercicio = new EnumMap<>(Ejercicio.class);
        if (levantamientos == null) return porEjercicio;

        for (Levantamiento l : levantamientos) {
            if (l.getEjercicio() == null || l.getPeso() == null || l.getRepeticiones() == null) continue;

            double estimado = maximoEstimado(l.getPeso(), l.getRepeticiones());
            porEjercicio.merge(l.getEjercicio(), estimado, Math::max);
        }
        return porEjercicio;
    }

    /**
     * Frase para la explicacion del match, sin redactar.
     *
     * <p>Los nombres de los ejercicios se encadenan como mensajes en vez de
     * unirse con " y ": esa conjuncion tambien cambia de idioma, y aqui no
     * sabemos en cual se va a leer.
     */
    public static Mensaje describir(Comparacion comparacion) {
        if (!comparacion.hayDatos()) return Mensaje.de("fuerza.sinDatos");

        Mensaje ejercicios = enumerarEjercicios(comparacion);

        if (comparacion.ratio() >= 0.9) return Mensaje.de("fuerza.igual", ejercicios);
        if (comparacion.ratio() >= 0.6) return Mensaje.de("fuerza.parecida", ejercicios);
        if (comparacion.ratio() >= 0.25) return Mensaje.de("fuerza.diferencia", ejercicios);
        return Mensaje.de("fuerza.muyDistintos");
    }

    private static Mensaje enumerarEjercicios(Comparacion comparacion) {
        var nombres = comparacion.comunes().stream().map(Enum::name).toList();
        if (nombres.isEmpty()) return Mensaje.de("lista.vacia");

        // Comas hasta el penultimo y la conjuncion solo al final. Encadenando
        // "lista.dos" en cada vuelta salia "Sentadilla y Press de banca y Peso
        // muerto", que es como habla un generador, no una persona.
        Mensaje acumulado = Mensaje.de("ejercicio." + nombres.get(0));
        for (int i = 1; i < nombres.size() - 1; i++) {
            acumulado = Mensaje.de("lista.coma", acumulado, Mensaje.de("ejercicio." + nombres.get(i)));
        }
        if (nombres.size() == 1) return acumulado;
        return Mensaje.de("lista.dos", acumulado,
                Mensaje.de("ejercicio." + nombres.get(nombres.size() - 1)));
    }
}
