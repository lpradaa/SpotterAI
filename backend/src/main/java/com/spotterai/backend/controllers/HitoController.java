package com.spotterai.backend.controllers;

import com.spotterai.backend.dtos.HitoDTO;
import com.spotterai.backend.services.HitoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Marcas y logros del perfil.
 *
 * El medio se sube aparte, en {@link MedioController}, y aquí llega ya como
 * ruta: así una foto que falla no tira el hito y se puede reintentar solo la
 * subida.
 */
@RestController
@RequestMapping("/api/hitos")
public class HitoController {

    private final HitoService hitoService;

    public HitoController(HitoService hitoService) {
        this.hitoService = hitoService;
    }

    private String emailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /** Cuerpo de creación. La fecha llega como texto ISO o no llega. */
    public record NuevoHito(String titulo, String descripcion, String fecha,
                            String medioUrl, String medioTipo) {}

    @GetMapping
    public ResponseEntity<List<HitoDTO>> mios() {
        return ResponseEntity.ok(hitoService.mios(emailAutenticado()));
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody NuevoHito cuerpo) {
        try {
            LocalDate fecha = cuerpo.fecha() == null || cuerpo.fecha().isBlank()
                    ? null : LocalDate.parse(cuerpo.fecha());

            return ResponseEntity.ok(hitoService.crear(emailAutenticado(),
                    cuerpo.titulo(), cuerpo.descripcion(), fecha,
                    cuerpo.medioUrl(), cuerpo.medioTipo()));
        } catch (IllegalArgumentException e) {
            // Cubre también una fecha con formato imposible, que DateTimeParseException
            // hereda de aquí.
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{hitoId}")
    public ResponseEntity<?> borrar(@PathVariable Long hitoId) {
        try {
            hitoService.borrar(emailAutenticado(), hitoId);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
