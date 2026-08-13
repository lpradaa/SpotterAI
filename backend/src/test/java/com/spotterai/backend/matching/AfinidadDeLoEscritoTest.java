package com.spotterai.backend.matching;

import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.semantica.VectorDeTexto;
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

    /** Un usuario con un vector de biografia construido a mano. */
    private static Usuario conVector(double semilla) {
        Usuario u = usuario();
        u.setBiografia("da igual, lo que compara el motor es el vector");
        u.setBiografiaVector(vector(semilla).aBytes());
        u.setBiografiaVectorDe("huella" + semilla);
        return u;
    }

    /** Vectores deterministas y normalizados, como los del servicio real. */
    private static VectorDeTexto vector(double semilla) {
        float[] v = new float[VectorDeTexto.DIMENSIONES];
        for (int i = 0; i < v.length; i++) v[i] = (float) Math.sin(semilla + i * 0.1);

        double suma = 0;
        for (float x : v) suma += x * x;
        float norma = (float) Math.sqrt(suma);
        for (int i = 0; i < v.length; i++) v[i] /= norma;

        return new VectorDeTexto(v);
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
        assertThat(afinidadDe(conVector(1), usuario()).aplicable()).isFalse();
    }

    @Test
    @DisplayName("un vector guardado por otro modelo se trata como dato ausente")
    void vectorDeOtroModelo() {
        Usuario viejo = usuario();
        viejo.setBiografia("tengo bio pero el vector es de antes");
        viejo.setBiografiaVector(new byte[]{1, 2, 3});

        // No revienta el calculo de nadie: cae en "no lo sabemos".
        assertThat(afinidadDe(viejo, conVector(1)).aplicable()).isFalse();
    }

    // ===================== La señal =====================

    @Test
    @DisplayName("dos biografías idénticas dan el máximo del factor")
    void identicas() {
        Usuario uno = conVector(2);
        Usuario otro = conVector(2);

        FactorCompatibilidad afinidad = afinidadDe(uno, otro);

        assertThat(afinidad.aplicable()).isTrue();
        assertThat(afinidad.ratio()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("parecerse más da más puntos que parecerse menos")
    void masParecidoPuntuaMas() {
        Usuario yo = conVector(0);

        double cerca = afinidadDe(yo, conVector(0.05)).puntos();
        double lejos = afinidadDe(yo, conVector(3.0)).puntos();

        assertThat(cerca).isGreaterThan(lejos);
    }

    // ===================== El techo =====================

    @Test
    @DisplayName("la afinidad no puede tapar que no coincidáis en horario")
    void noTapaElHorario() {
        Usuario uno = conVector(5);
        Usuario otro = conVector(5); // biografias identicas: el maximo del factor

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
        FactorCompatibilidad afinidad = afinidadDe(conVector(1), conVector(1));

        // Se comprueba contra el resto para que la prueba siga significando algo
        // si alguien reajusta el reparto: lo que se defiende no es el numero 6,
        // es que una señal blanda de un modelo no pese como el horario.
        double horario = afinidadDe(conVector(1), conVector(1)) != null
                ? CalculadoraCompatibilidad.PESO_HORARIO : 0;

        assertThat(CalculadoraCompatibilidad.PESO_AFINIDAD).isEqualTo(6);
        assertThat(CalculadoraCompatibilidad.PESO_AFINIDAD).isLessThan(horario / 4);
        assertThat(afinidad.puntosMax()).isPositive();
    }
}
