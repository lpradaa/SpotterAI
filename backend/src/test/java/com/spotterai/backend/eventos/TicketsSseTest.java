package com.spotterai.backend.eventos;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El ticket es lo único que protege el canal de eventos, porque ese endpoint
 * queda fuera del filtro JWT. Sus dos garantías —un solo uso y caducidad— son
 * justo lo que hace que valga menos que un token si se filtra por la URL, así
 * que conviene tenerlas fijadas por pruebas.
 */
class TicketsSseTest {

    /** Clock que se puede adelantar a mano: probar caducidades durmiendo es lento y frágil. */
    private static class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-01-01T10:00:00Z");

        void avanzar(Duration cuanto) {
            ahora = ahora.plus(cuanto);
        }

        @Override public Instant instant() { return ahora; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zona) { return this; }
    }

    @Test
    void unTicketReciénCreadoDevuelveSuUsuario() {
        TicketsSse tickets = new TicketsSse(new RelojManual());

        String ticket = tickets.crear(42L);

        assertEquals(42L, tickets.canjear(ticket).orElseThrow());
    }

    @Test
    void unTicketSoloSePuedeCanjearUnaVez() {
        TicketsSse tickets = new TicketsSse(new RelojManual());
        String ticket = tickets.crear(42L);

        tickets.canjear(ticket);

        assertTrue(tickets.canjear(ticket).isEmpty(),
                "Si valiera dos veces, cualquiera que lo viera en un log podría abrir el canal");
    }

    @Test
    void unTicketCaducadoNoVale() {
        RelojManual reloj = new RelojManual();
        TicketsSse tickets = new TicketsSse(reloj);
        String ticket = tickets.crear(42L);

        reloj.avanzar(TicketsSse.VALIDEZ.plusSeconds(1));

        assertTrue(tickets.canjear(ticket).isEmpty());
    }

    @Test
    void unTicketInventadoNoVale() {
        TicketsSse tickets = new TicketsSse(new RelojManual());

        assertTrue(tickets.canjear("inventado").isEmpty());
        assertTrue(tickets.canjear(null).isEmpty());
        assertTrue(tickets.canjear("  ").isEmpty());
    }

    @Test
    void losTicketsQueNadieCanjeaNoSeAcumulan() {
        RelojManual reloj = new RelojManual();
        TicketsSse tickets = new TicketsSse(reloj);

        // Alguien abre y cierra la página diez veces sin llegar a conectar.
        for (int i = 0; i < 10; i++) {
            tickets.crear(42L);
        }
        assertEquals(10, tickets.pendientes());

        reloj.avanzar(TicketsSse.VALIDEZ.plusSeconds(1));
        tickets.crear(42L);

        assertEquals(1, tickets.pendientes(),
                "Sin purga, cada ticket no canjeado se quedaría en memoria para siempre");
    }
}
