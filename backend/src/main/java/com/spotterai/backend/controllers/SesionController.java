package com.spotterai.backend.controllers;

import com.spotterai.backend.dtos.NuevaSesionDTO;
import com.spotterai.backend.dtos.SesionDTO;
import com.spotterai.backend.dtos.SugerenciaSesionDTO;
import com.spotterai.backend.services.SesionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Quedar para entrenar: proponer, responder y apuntar lo que salió.
 *
 * <p>Los errores se devuelven separados a propósito: 403 cuando la acción no te
 * corresponde (responder por otro, quedar con quien no es tu compañero) y 400
 * cuando lo que pides no tiene sentido (el pasado, una hora imposible). Son dos
 * cosas distintas y la interfaz las trata distinto.
 */
@RestController
@RequestMapping("/api/sesiones")
public class SesionController {

    private final SesionService sesionService;

    public SesionController(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    private String emailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /** GET /api/sesiones — lo que sigue contando: por responder, por delante y por apuntar. */
    @GetMapping
    public ResponseEntity<List<SesionDTO>> mias() {
        return ResponseEntity.ok(sesionService.mias(emailAutenticado()));
    }

    /**
     * GET /api/sesiones/sugerencia/{otroUsuarioId}
     *
     * Lo que el formulario trae ya puesto, sacado del solape real.
     */
    @GetMapping("/sugerencia/{otroUsuarioId}")
    public ResponseEntity<?> sugerencia(@PathVariable Long otroUsuarioId) {
        try {
            SugerenciaSesionDTO sugerencia = sesionService.sugerir(emailAutenticado(), otroUsuarioId);
            return ResponseEntity.ok(sugerencia);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/sesiones/con/{otroUsuarioId}
     *
     * La sesión viva con esa persona, para enseñarla dentro del chat. Devuelve
     * 204 cuando no hay ninguna, que no es un error: es lo normal.
     */
    @GetMapping("/con/{otroUsuarioId}")
    public ResponseEntity<?> conMigo(@PathVariable Long otroUsuarioId) {
        return sesionService.conMigo(emailAutenticado(), otroUsuarioId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** POST /api/sesiones/proponer/{otroUsuarioId} */
    @PostMapping("/proponer/{otroUsuarioId}")
    public ResponseEntity<?> proponer(@PathVariable Long otroUsuarioId,
                                      @RequestBody NuevaSesionDTO cuerpo) {
        try {
            return ResponseEntity.ok(sesionService.proponer(emailAutenticado(), otroUsuarioId, cuerpo));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/sesiones/{id}/aceptar */
    @PostMapping("/{id}/aceptar")
    public ResponseEntity<?> aceptar(@PathVariable Long id) {
        return responder(id, true);
    }

    /** POST /api/sesiones/{id}/rechazar */
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable Long id) {
        return responder(id, false);
    }

    private ResponseEntity<?> responder(Long id, boolean acepta) {
        try {
            return ResponseEntity.ok(sesionService.responder(emailAutenticado(), id, acepta));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/sesiones/{id} — cancelar, mientras no haya empezado. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        try {
            sesionService.cancelar(emailAutenticado(), id);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/sesiones/{id}/confirmar
     *
     * "Sí, entrenamos". Apunta el entrenamiento en tu historial, solo en el tuyo.
     */
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<?> confirmar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sesionService.confirmar(emailAutenticado(), id));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
