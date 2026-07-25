package com.spotterai.backend.eventos;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tickets de un solo uso para abrir la conexión de eventos.
 *
 * El motivo de que esto exista: EventSource, la API del navegador para SSE, no
 * permite añadir cabeceras, así que no hay forma de mandar el Authorization con
 * el JWT. La salida cómoda sería poner el token en la URL, pero los parámetros
 * de consulta acaban en los logs de acceso, en el historial y en el Referer, y
 * ahí el token vale para cualquier endpoint durante toda su vida.
 *
 * Un ticket, en cambio, se canjea una sola vez, caduca en medio minuto y no
 * sirve para nada más que abrir el canal: si se filtra, no vale nada.
 */
@Service
public class TicketsSse {

    static final Duration VALIDEZ = Duration.ofSeconds(30);

    private record Ticket(Long usuarioId, Instant caduca) {}

    private final Map<String, Ticket> emitidos = new ConcurrentHashMap<>();
    private final Clock reloj;

    public TicketsSse() {
        this(Clock.systemUTC());
    }

    /** Para las pruebas: permite adelantar el reloj sin esperar. */
    TicketsSse(Clock reloj) {
        this.reloj = reloj;
    }

    public String crear(Long usuarioId) {
        purgarCaducados();
        String ticket = UUID.randomUUID().toString();
        emitidos.put(ticket, new Ticket(usuarioId, reloj.instant().plus(VALIDEZ)));
        return ticket;
    }

    /**
     * Devuelve el usuario del ticket y lo invalida. Vacío si no existe, si ya se
     * usó o si ha caducado; quien llama no necesita distinguir los tres casos.
     */
    public Optional<Long> canjear(String ticket) {
        if (ticket == null || ticket.isBlank()) return Optional.empty();

        Ticket emitido = emitidos.remove(ticket);
        if (emitido == null) return Optional.empty();

        return emitido.caduca().isAfter(reloj.instant())
                ? Optional.of(emitido.usuarioId())
                : Optional.empty();
    }

    /**
     * Los tickets que se piden y no se canjean se quedarían para siempre. Como
     * caducan en segundos, basta con barrer al emitir uno nuevo.
     */
    private void purgarCaducados() {
        Instant ahora = reloj.instant();
        emitidos.values().removeIf(t -> !t.caduca().isAfter(ahora));
    }

    /** Solo para pruebas. */
    int pendientes() {
        return emitidos.size();
    }
}
