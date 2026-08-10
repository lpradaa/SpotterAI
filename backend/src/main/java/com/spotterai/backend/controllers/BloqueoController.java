package com.spotterai.backend.controllers;

import com.spotterai.backend.seguridad.Bloqueos;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Bloquear y desbloquear.
 *
 * <p>No hay endpoint para saber si alguien te ha bloqueado a ti, y no es un
 * olvido: eso convertiria el bloqueo en una notificacion. Desde el lado del
 * bloqueado, la otra persona simplemente ha dejado de existir en la aplicacion.
 */
@RestController
@RequestMapping("/api/bloqueos")
public class BloqueoController {

    private final Bloqueos bloqueos;

    public BloqueoController(Bloqueos bloqueos) {
        this.bloqueos = bloqueos;
    }

    private String yo() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /** A quién has bloqueado tú. Los únicos que puedes quitar. */
    @GetMapping
    public ResponseEntity<?> mios() {
        List<Map<String, Object>> lista = bloqueos.mios(yo()).stream()
                .map(b -> Map.<String, Object>of(
                        "usuarioId", b.getBloqueado().getId(),
                        "nombre", b.getBloqueado().getNombre(),
                        "desde", b.getCreadoEn()))
                .toList();

        return ResponseEntity.ok(lista);
    }

    @PostMapping("/{otroUsuarioId}")
    public ResponseEntity<?> bloquear(@PathVariable Long otroUsuarioId) {
        try {
            bloqueos.bloquear(yo(), otroUsuarioId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{otroUsuarioId}")
    public ResponseEntity<?> desbloquear(@PathVariable Long otroUsuarioId) {
        bloqueos.desbloquear(yo(), otroUsuarioId);
        return ResponseEntity.noContent().build();
    }
}
