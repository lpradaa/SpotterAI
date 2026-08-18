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
        // La fecha con formato imposible tambien sale por aqui:
        // DateTimeParseException hereda de IllegalArgumentException, asi que la
        // recoge ManejadorDeErrores igual que el resto.
        LocalDate fecha = cuerpo.fecha() == null || cuerpo.fecha().isBlank()
                ? null : LocalDate.parse(cuerpo.fecha());

        return ResponseEntity.ok(hitoService.crear(emailAutenticado(),
                cuerpo.titulo(), cuerpo.descripcion(), fecha,
                cuerpo.medioUrl(), cuerpo.medioTipo()));
    }

    @DeleteMapping("/{hitoId}")
    public ResponseEntity<?> borrar(@PathVariable Long hitoId) {
        hitoService.borrar(emailAutenticado(), hitoId);
        return ResponseEntity.noContent().build();
    }
}
