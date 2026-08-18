package com.spotterai.backend.matching;

import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El noveno factor: cuanto se parece lo que cada uno escribe sobre si mismo.
 *
 * <p>Lo que se fija aqui no es que el modelo acierte —eso se mide con
 * biografias reales en {@code embeddings/calibracion}— sino las tres decisiones
 * de producto que hacen que este factor sea defendible:
 *
 * <ol>
 *   <li><b>El modelo no decide el orden.</b> Aporta una señal ponderada como
 *       cualquier otra. Un modelo puntuando da un producto que no se puede
 *       depurar ni defender.</li>
 *   <li><b>Sin biografia no es incompatibilidad, es falta de datos.</b> Quien no
 *       ha escrito nada no puede salir penalizado frente a quien si.</li>
 *   <li><b>El techo esta acotado.</b> Por muy parecidos que sean dos textos, la
 *       afinidad no puede tapar que dos personas no coincidan en horario.</li>
 * </ol>
 */
class AfinidadDeLoEscritoTest {

    private static Usuario usuario() {
        Usuario u = new Usuario();
        u.setNombre("Test");
        return u;
    }

    /**
     * Un usuario con sus tres ejes puestos a mano.
     *
     * <p>Antes esto ponia un vector de 384 numeros. Desde la V19 el factor lee
     * tres posiciones con nombre, porque comparar los vectores resulto ordenar
     * por parecido de redaccion y no por compatibilidad — medido en
     * {@code docs/medir-el-motor.md}.
     */
    private static Usuario conEjes(Double exigencia, Double ambicion, Double flexibilidad) {
        Usuario u = usuario();
        u.setBiografia("da igual: lo que compara el motor son los ejes");
        u.setIntencionExigencia(exigencia);
        u.setIntencionAmbicion(ambicion);
        u.setIntencionFlexibilidad(flexibilidad);
        u.setIntencionesDe("huella");
        return u;
    }

    /** Los tres ejes iguales, para el caso corriente. */
    private static Usuario conEjes(double todos) {
        return conEjes(todos, todos, todos);
    }

    private static Disponibilidad franja(String dia, String inicio, String fin) {
        return new Disponibilidad(dia, LocalTime.parse(inicio), LocalTime.parse(fin), null, true);
    }

    private static FactorCompatibilidad afinidadDe(Usuario a, Usuario b) {
        List<Disponibilidad> horario = List.of(franja("Lunes", "18:00", "20:00"));
        return CalculadoraCompatibilidad
                .calcular(PerfilDeMatch.de(a, horario), PerfilDeMatch.de(b, horario))
                .factores().stream()
                .filter(f -> f.nombre().equals("afinidad"))
                .findFirst().orElseThrow();
    }

    // ===================== Falta de datos, no incompatibilidad =====================

    @Test
    @DisplayName("sin biografía el factor no puntúa cero: no se evalúa")
    void sinBiografia() {
        FactorCompatibilidad afinidad = afinidadDe(usuario(), usuario());

        // La diferencia entre "no aplicable" y "cero puntos" es la decision mas
        // importante del motor entero: un cero se lee como mal encaje y penaliza
        // a quien no rellena un campo. Aqui vale doble, porque escribir una
        // biografia cuesta mas que elegir de un desplegable.
        assertThat(afinidad.aplicable()).isFalse();
        assertThat(afinidad.puntosMax()).isZero();
    }

    @Test
    @DisplayName("si solo uno ha escrito, tampoco hay nada que comparar")
    void soloUnoEscribe() {
        assertThat(afinidadDe(conEjes(0.8), usuario()).aplicable()).isFalse();
    }

    @Test
    @DisplayName("una biografía que no habla de ningún eje se trata como dato ausente")
    void biografiaQueNoDiceNada() {
        Usuario callado = usuario();
        callado.setBiografia("Entreno los lunes y los miércoles a las siete.");
        // Habla de horario, que ya es un campo del perfil: de los tres ejes, nada.

        assertThat(afinidadDe(callado, conEjes(0.8)).aplicable()).isFalse();
    }

    @Test
    @DisplayName("solo cuentan los ejes de los que han hablado los dos")
    void unEjeSueltoBasta() {
        // Uno solo habla de ambición, el otro de los tres. El unico eje comun
        // es la ambicion, y ahi coinciden: el factor se evalua con ese.
        Usuario parco = conEjes(null, 0.7, null);
        FactorCompatibilidad afinidad = afinidadDe(parco, conEjes(0.7));

        assertThat(afinidad.aplicable()).isTrue();
        assertThat(afinidad.ratio()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("querer lo contrario puntúa menos que querer lo mismo")
    void loContrarioPuntuaMenos() {
        // Es exactamente el caso que el factor anterior hacia al reves: dos
        // biografias opuestas escritas parecido sacaban mas que dos compatibles
        // escritas distinto.
        double opuestos = afinidadDe(conEjes(0.9), conEjes(-0.9)).puntos();
        double iguales = afinidadDe(conEjes(0.9), conEjes(0.9)).puntos();

        assertThat(opuestos).isLessThan(iguales);
    }

    // ===================== La señal =====================

    @Test
    @DisplayName("dos biografías idénticas dan el máximo del factor")
    void identicas() {
        Usuario uno = conEjes(0.6);
        Usuario otro = conEjes(0.6);

        FactorCompatibilidad afinidad = afinidadDe(uno, otro);

        assertThat(afinidad.aplicable()).isTrue();
        assertThat(afinidad.ratio()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("parecerse más da más puntos que parecerse menos")
    void masParecidoPuntuaMas() {
        Usuario yo = conEjes(0.5);

        double cerca = afinidadDe(yo, conEjes(0.45)).puntos();
        double lejos = afinidadDe(yo, conEjes(-0.6)).puntos();

        assertThat(cerca).isGreaterThan(lejos);
    }

    // ===================== El techo =====================

    @Test
    @DisplayName("la afinidad no puede tapar que no coincidáis en horario")
    void noTapaElHorario() {
        Usuario uno = conEjes(0.7);
        Usuario otro = conEjes(0.7); // los mismos ejes: el maximo del factor

        // Pero entrenan a horas que no se cruzan.
        PuntuacionCompatibilidad sinSolape = CalculadoraCompatibilidad.calcular(
                PerfilDeMatch.de(uno, List.of(franja("Lunes", "07:00", "09:00"))),
                PerfilDeMatch.de(otro, List.of(franja("Lunes", "20:00", "22:00"))));

        // El horario vale 40 y la afinidad 6. Que dos personas se describan
        // igual no las pone a entrenar juntas si no coinciden nunca, y el numero
        // tiene que decirlo.
        assertThat(sinSolape.total()).isLessThan(50);
    }

    @Test
    @DisplayName("el factor pesa 6 y no más")
    void elPesoEsElQueEs() {
        FactorCompatibilidad afinidad = afinidadDe(conEjes(0.4), conEjes(0.4));

        // Se comprueba contra el resto para que la prueba siga significando algo
        // si alguien reajusta el reparto: lo que se defiende no es el numero 6,
        // es que una señal blanda de un modelo no pese como el horario.
        double horario = afinidadDe(conEjes(0.4), conEjes(0.4)) != null
                ? CalculadoraCompatibilidad.PESO_HORARIO : 0;

        assertThat(CalculadoraCompatibilidad.PESO_AFINIDAD).isEqualTo(6);
        assertThat(CalculadoraCompatibilidad.PESO_AFINIDAD).isLessThan(horario / 4);
        assertThat(afinidad.puntosMax()).isPositive();
    }
}
