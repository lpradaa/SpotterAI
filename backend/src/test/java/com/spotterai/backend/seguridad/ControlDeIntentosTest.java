package com.spotterai.backend.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El freno a la fuerza bruta.
 *
 * <p>El reloj se mueve a mano. Una prueba de "a los quince minutos se libera"
 * que de verdad esperase quince minutos no la ejecutaria nadie, y una que no lo
 * comprobase dejaria sin cubrir justo la mitad del comportamiento: bloquear es
 * facil, lo dificil es volver a abrir.
 */
class ControlDeIntentosTest {

    /** Reloj que se adelanta cuando se le dice. */
    private static final class RelojManual extends Clock {
        private Instant momento = Instant.parse("2026-07-01T10:00:00Z");

        void avanzar(Duration cuanto) { momento = momento.plus(cuanto); }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zona) { return this; }
        @Override public Instant instant() { return momento; }
    }

    private final RelojManual reloj = new RelojManual();
    private final ControlDeIntentos control = new ControlDeIntentos(reloj);

    private final String correo = ControlDeIntentos.claveDeCorreo("alguien@test.com");
    private final String direccion = ControlDeIntentos.claveDeDireccion("10.0.0.1");

    private void fallar(String clave, int veces) {
        for (int i = 0; i < veces; i++) control.fallo(clave);
    }

    @Test
    @DisplayName("Sin fallos previos se puede entrar")
    void puertaAbierta() {
        assertFalse(control.bloqueada(correo));
        assertFalse(control.bloqueada(direccion));
    }

    @Test
    @DisplayName("Se cierra al agotar el cupo, y ni uno antes")
    void seCierraJustoAlLimite() {
        fallar(correo, ControlDeIntentos.MAX_POR_CORREO - 1);
        assertFalse(control.bloqueada(correo), "Con un intento de margen todavia se puede probar");

        control.fallo(correo);
        assertTrue(control.bloqueada(correo));
    }

    @Test
    @DisplayName("Una dirección aguanta mucho más que un correo")
    void laDireccionEsMasPermisiva() {
        // Detras de una IP puede haber un gimnasio entero: apretarla igual que
        // el correo dejaria fuera a gente que no ha hecho nada.
        fallar(direccion, ControlDeIntentos.MAX_POR_CORREO);
        assertFalse(control.bloqueada(direccion));

        fallar(direccion, ControlDeIntentos.MAX_POR_DIRECCION - ControlDeIntentos.MAX_POR_CORREO);
        assertTrue(control.bloqueada(direccion));
    }

    @Test
    @DisplayName("Pasada la ventana vuelve a abrirse sola")
    void seLibera() {
        fallar(correo, ControlDeIntentos.MAX_POR_CORREO);
        assertTrue(control.bloqueada(correo));

        reloj.avanzar(ControlDeIntentos.VENTANA.plusSeconds(1));

        assertFalse(control.bloqueada(correo), "Bloquear para siempre convierte el freno en un castigo");
        assertEquals(0, control.fallosDe(correo));
    }

    @Test
    @DisplayName("Los fallos viejos no se suman a los nuevos")
    void laVentanaEsDeslizante() {
        fallar(correo, ControlDeIntentos.MAX_POR_CORREO - 1);
        reloj.avanzar(ControlDeIntentos.VENTANA.plusSeconds(1));

        // Cuatro fallos de hace media hora mas uno de ahora no son cinco: quien
        // se equivoca de vez en cuando no deberia acabar bloqueado nunca.
        control.fallo(correo);

        assertFalse(control.bloqueada(correo));
        assertEquals(1, control.fallosDe(correo));
    }

    @Test
    @DisplayName("Entrar bien borra lo que hubiera")
    void elAciertoPerdona() {
        fallar(correo, ControlDeIntentos.MAX_POR_CORREO - 1);

        control.acierto(correo);

        assertEquals(0, control.fallosDe(correo));
        assertFalse(control.bloqueada(correo));
    }

    @Test
    @DisplayName("Se dice cuánto queda, para poder contarlo")
    void laEsperaSeSabe() {
        control.fallo(correo);
        reloj.avanzar(Duration.ofMinutes(5));

        Duration espera = control.esperaDe(correo);

        assertEquals(ControlDeIntentos.VENTANA.minusMinutes(5), espera);
    }

    @Test
    @DisplayName("Bloquear un correo no bloquea a los demás")
    void cadaCuentaVaAparte() {
        fallar(correo, ControlDeIntentos.MAX_POR_CORREO);

        assertTrue(control.bloqueada(correo));
        assertFalse(control.bloqueada(ControlDeIntentos.claveDeCorreo("otra@test.com")));
    }

    @Test
    @DisplayName("El correo se lee igual escrito de cualquier forma")
    void mayusculasYEspaciosDaIgual() {
        // Sin normalizar, cambiar una mayuscula daria cinco intentos mas por
        // cada variante, que es tanto como no tener freno.
        assertEquals(ControlDeIntentos.claveDeCorreo("Alguien@Test.com "),
                     ControlDeIntentos.claveDeCorreo("alguien@test.com"));
    }

    @Test
    @DisplayName("La limpieza tira lo caducado y respeta lo vivo")
    void laLimpiezaNoSePasa() {
        fallar(correo, 2);
        reloj.avanzar(ControlDeIntentos.VENTANA.plusSeconds(1));
        fallar(direccion, 2);

        control.limpiar();

        // Sin limpieza, el mapa crece con cada correo inventado y el propio
        // freno pasa a ser la forma mas comoda de tumbar el servidor.
        assertEquals(0, control.fallosDe(correo));
        assertEquals(2, control.fallosDe(direccion));
    }
}
