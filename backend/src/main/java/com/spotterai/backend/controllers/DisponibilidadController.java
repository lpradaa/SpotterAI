package com.spotterai.backend.controllers;

import com.spotterai.backend.dtos.DisponibilidadDTO;
import com.spotterai.backend.services.DisponibilidadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disponibilidad")
@CrossOrigin(origins = "http://localhost:4200")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    public DisponibilidadController(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    // Método de utilidad para sacar el email del Token JWT sin repetir código
    private String obtenerEmailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * OBTENER EL CALENDARIO DEL USUARIO
     * GET http://localhost:8080/api/disponibilidad
     */
    @GetMapping
    public ResponseEntity<List<DisponibilidadDTO>> obtenerMisHorarios() {
        String email = obtenerEmailAutenticado();
        return ResponseEntity.ok(disponibilidadService.obtenerHorariosDeUsuario(email));
    }

    /**
     * AÑADIR UN NUEVO HORARIO
     * POST http://localhost:8080/api/disponibilidad
     */
    @PostMapping
    public ResponseEntity<?> agregarHorario(@RequestBody DisponibilidadDTO dto) {
        try {
            String email = obtenerEmailAutenticado();
            DisponibilidadDTO nuevoHorario = disponibilidadService.agregarHorario(email, dto);
            return ResponseEntity.ok(nuevoHorario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * BORRAR UN HORARIO
     * DELETE http://localhost:8080/api/disponibilidad/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarHorario(@PathVariable Long id) {
        try {
            String email = obtenerEmailAutenticado();
            disponibilidadService.eliminarHorario(email, id);
            return ResponseEntity.ok().body("Horario eliminado correctamente.");
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage()); // 403 Forbidden
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}