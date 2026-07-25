package com.spotterai.backend.controllers;

import com.spotterai.backend.eventos.CanalEventos;
import com.spotterai.backend.eventos.TicketsSse;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;

/**
 * Canal de eventos en tiempo real.
 *
 * Son dos pasos a propósito: se pide un ticket con el JWT por la vía normal y
 * después se abre el canal con ese ticket. El porqué está en {@link TicketsSse}.
 */
@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final CanalEventos canalEventos;
    private final TicketsSse tickets;
    private final UsuarioRepository usuarioRepository;

    public EventoController(CanalEventos canalEventos, TicketsSse tickets, UsuarioRepository usuarioRepository) {
        this.canalEventos = canalEventos;
        this.tickets = tickets;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * POST /api/eventos/ticket
     *
     * Protegido por el filtro JWT como cualquier otro endpoint.
     */
    @PostMapping("/ticket")
    public ResponseEntity<?> pedirTicket() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);
        if (usuario.isEmpty()) {
            return ResponseEntity.status(401).body("Usuario no encontrado.");
        }

        return ResponseEntity.ok(Map.of("ticket", tickets.crear(usuario.get().getId())));
    }

    /**
     * GET /api/eventos/suscribir?ticket=...
     *
     * Queda fuera del filtro JWT porque EventSource no puede mandar cabeceras;
     * el ticket es la credencial y solo sirve para esto.
     */
    @GetMapping(value = "/suscribir", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> suscribir(@RequestParam String ticket) {
        Optional<Long> usuarioId = tickets.canjear(ticket);
        if (usuarioId.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(canalEventos.abrir(usuarioId.get()));
    }
}
