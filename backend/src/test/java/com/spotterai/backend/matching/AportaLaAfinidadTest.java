package com.spotterai.backend.matching;

import com.spotterai.backend.textos.Mensaje;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Que cambia de verdad el noveno factor.
 *
 * <h2>La pregunta</h2>
 *
 * <p>La afinidad de lo escrito vale 6 puntos y esta razonada con detalle en
 * {@link CalculadoraCompatibilidad}: es lo unico que mira lo que una persona
 * escribe con sus palabras, y hay cosas que ninguna casilla recoge. Tambien es
 * el unico factor que necesita un modelo levantado, un servicio aparte y 475 MB
 * de memoria.
 *
 * <p>Eso obliga a hacerle una pregunta que a los otros ocho no: <b>¿cuantas
 * decisiones cambia?</b> Un factor blando que costara todo eso y no moviera
 * nada seria un adorno caro.
 *
 * <h2>Como se mide</h2>
 *
 * <p>La misma poblacion dos veces, con biografia y sin ella, y se cuenta que
 * cambia. Sin biografia el factor se queda sin datos y sus 6 puntos se reparten
 * entre los demas, que es exactamente lo que pasaria si se quitara.
 *
 * <p>Y se cuenta en decisiones, no en puntos: cuantas parejas cambian de tramo
 * —lo que la pantalla llama "muy compatibles" o "buena compatibilidad"— y
 * cuantas cambian de factor dominante, que es la frase que se enseña debajo del
 * numero. Es la misma metrica con la que se calibro la cuantizacion del modelo:
 * lo que importa no es cuanto se mueve un vector, es cuanto se mueve lo que la
 * persona lee.
 */
class AportaLaAfinidadTest {

    private static final int CUANTOS = 60;

    /**
     * Lo que vale el factor, y por tanto el techo de lo que puede mover.
     *
     * <p>Esta escrito aqui y no leido de la calculadora a proposito: si un dia
     * alguien le sube el peso, esta prueba tiene que fallar y obligar a mirar el
     * analisis otra vez, no adaptarse en silencio al valor nuevo.
     */
    private static final int PESO_DE_LA_AFINIDAD = 6;

    private static List<PerfilDeMatch> conBiografia;
    private static List<PerfilDeMatch> sinBiografia;

    @BeforeAll
    static void prepararLasDosPoblaciones() {
        // La misma semilla en las dos: es la misma gente, con y sin lo escrito.
        conBiografia = BancoDePerfiles.poblacion(CUANTOS, true);
        sinBiografia = BancoDePerfiles.poblacion(CUANTOS, false);
    }

    private static String tramo(int total) {
        if (total >= 85) return "excelente";
        if (total >= 70) return "muy";
        if (total >= 50) return "buena";
        if (total >= 30) return "parcial";
        return "poca";
    }

    private static List<PuntuacionCompatibilidad> parejas(List<PerfilDeMatch> gente) {
        List<PuntuacionCompatibilidad> puntuaciones = new ArrayList<>();
        for (int i = 0; i < gente.size(); i++) {
            for (int j = i + 1; j < gente.size(); j++) {
                puntuaciones.add(CalculadoraCompatibilidad.calcular(gente.get(i), gente.get(j)));
            }
        }
        return puntuaciones;
    }

    private static String dominante(PuntuacionCompatibilidad p) {
        FactorCompatibilidad f = p.factorDominante();
        return f == null ? "ninguno" : f.nombre();
    }

    @Test
    @DisplayName("Informe: que cambia el noveno factor")
    void informe() {
        List<PuntuacionCompatibilidad> con = parejas(conBiografia);
        List<PuntuacionCompatibilidad> sin = parejas(sinBiografia);

        int cambianDeTramo = 0;
        int cambianDeFrase = 0;
        int sumaDeDiferencias = 0;
        int maximaDiferencia = 0;

        for (int i = 0; i < con.size(); i++) {
            int diferencia = Math.abs(con.get(i).total() - sin.get(i).total());
            sumaDeDiferencias += diferencia;
            maximaDiferencia = Math.max(maximaDiferencia, diferencia);

            if (!tramo(con.get(i).total()).equals(tramo(sin.get(i).total()))) cambianDeTramo++;
            if (!dominante(con.get(i)).equals(dominante(sin.get(i)))) cambianDeFrase++;
        }

        System.out.println();
        System.out.println("=== Que aporta la afinidad de lo escrito ===");
        System.out.printf("Poblacion: %d perfiles, %d parejas (semilla %d)%n",
                CUANTOS, con.size(), BancoDePerfiles.SEMILLA);
        System.out.printf("  diferencia media           %.2f puntos%n",
                (double) sumaDeDiferencias / con.size());
        System.out.printf("  diferencia maxima          %d puntos%n", maximaDiferencia);
        System.out.printf("  cambian de tramo           %d  (%.1f %%)%n",
                cambianDeTramo, 100.0 * cambianDeTramo / con.size());
        System.out.printf("  cambian de frase           %d  (%.1f %%)%n",
                cambianDeFrase, 100.0 * cambianDeFrase / con.size());
        System.out.println();
        System.out.println("  'cambian de frase' = cambia el factor dominante, que es el texto");
        System.out.println("  que se enseña debajo del numero.");
        System.out.println();

        assertTrue(con.size() == sin.size(), "Las dos poblaciones tienen que ser la misma gente");
    }

    /**
     * El experimento se comprueba a si mismo antes de creerse el resultado.
     *
     * <p>La afinidad vale 6 puntos: si quitarla mueve la puntuacion mas de 6,
     * lo que se esta comparando no son dos versiones de la misma pareja, son
     * dos parejas distintas — y entonces el numero no mide lo que dice medir.
     *
     * <p>No es hipotetico. La primera version de esta medicion daba 54 puntos de
     * diferencia maxima y un 77 % de frases cambiadas, porque el sorteo de "esta
     * persona escribe biografia" se saltaba con un cortocircuito en la poblacion
     * sin biografia: la secuencia de numeros aleatorios se desplazaba y las dos
     * poblaciones dejaban de ser la misma gente. El resultado era imposible, y
     * por eso se vio. Esta comprobacion es lo que hace que se vea siempre.
     */
    @Test
    @DisplayName("Coherencia: quitar un factor de 6 puntos no puede mover mas de 6")
    void elExperimentoSeSostiene() {
        List<PuntuacionCompatibilidad> con = parejas(conBiografia);
        List<PuntuacionCompatibilidad> sin = parejas(sinBiografia);

        for (int i = 0; i < con.size(); i++) {
            int diferencia = Math.abs(con.get(i).total() - sin.get(i).total());

            assertTrue(diferencia <= PESO_DE_LA_AFINIDAD,
                    "La pareja " + i + " se mueve " + diferencia + " puntos al quitar un factor "
                            + "que vale " + PESO_DE_LA_AFINIDAD + ": las dos poblaciones no son "
                            + "la misma gente y la comparación no vale");
        }
    }

    /**
     * Seis puntos sobre cien tienen que notarse en algun sitio.
     *
     * <p>Si quitarlo no cambiara ninguna decision, el factor estaria pagando un
     * servicio aparte y 475 MB de memoria a cambio de nada, y la respuesta
     * correcta seria quitarlo — no subirle el peso.
     */
    @Test
    @DisplayName("Mueve decisiones: no es un adorno caro")
    void muevePeroPoco() {
        List<PuntuacionCompatibilidad> con = parejas(conBiografia);
        List<PuntuacionCompatibilidad> sin = parejas(sinBiografia);

        int cambios = 0;
        for (int i = 0; i < con.size(); i++) {
            if (!tramo(con.get(i).total()).equals(tramo(sin.get(i).total()))) cambios++;
        }

        assertTrue(cambios > 0,
                "Quitar la afinidad no cambia el tramo de ninguna pareja: entonces no está "
                        + "aportando nada y el servicio de embeddings no se paga solo");
    }

    /**
     * Y no puede mover demasiado.
     *
     * <p>Es una señal blanda —un modelo midiendo parecido entre dos frases
     * cortas— y el propio codigo dice que no puede pesar como el horario, que es
     * una restriccion dura. Si el factor decidiera la mitad de las parejas,
     * estaria mandando sobre datos duros, y eso seria un fallo de reparto por el
     * otro lado.
     */
    @Test
    @DisplayName("Pero no manda: sigue siendo una señal blanda")
    void noSeLeVaDeLasManos() {
        List<PuntuacionCompatibilidad> con = parejas(conBiografia);
        List<PuntuacionCompatibilidad> sin = parejas(sinBiografia);

        int cambios = 0;
        for (int i = 0; i < con.size(); i++) {
            if (!tramo(con.get(i).total()).equals(tramo(sin.get(i).total()))) cambios++;
        }
        double porcentaje = 100.0 * cambios / con.size();

        assertTrue(porcentaje < 40.0,
                "La afinidad cambia el tramo del " + porcentaje + " % de las parejas. Para una "
                        + "señal blanda de 6 puntos eso es mandar, no matizar");
    }

    /**
     * Cuando decide, se explica.
     *
     * <p>Si el factor cambiara la puntuacion pero nunca llegara a ser el
     * dominante, la pantalla enseñaria un numero movido por algo que no aparece
     * en ninguna frase. Y la tesis del producto es justo la contraria: que el
     * numero se pueda explicar.
     */
    @Test
    @DisplayName("Cuando decide, sale en la explicacion")
    void cuandoDecideSeExplica() {
        long vecesQueDomina = parejas(conBiografia).stream()
                .filter(p -> "afinidad".equals(dominante(p)))
                .count();

        assertTrue(vecesQueDomina > 0,
                "La afinidad nunca es el factor dominante, así que mueve el número sin aparecer "
                        + "nunca en la explicación: eso es justo lo que el producto no quiere");
    }
}
