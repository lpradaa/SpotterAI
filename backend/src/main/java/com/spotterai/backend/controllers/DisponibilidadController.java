package com.spotterai.backend.controllers;

import com.spotterai.backend.dtos.DisponibilidadDTO;
import com.spotterai.backend.services.DisponibilidadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disponibilidad")
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
        return ResponseEntity.ok(
                disponibilidadService.agregarHorario(obtenerEmailAutenticado(), dto));
    }

    /**
     * BORRAR UN HORARIO
     * DELETE http://localhost:8080/api/disponibilidad/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarHorario(@PathVariable Long id) {
        // Sin try: los errores los redacta ManejadorDeErrores, que es el unico
        // sitio que sabe el idioma de la peticion. Capturarlos aqui devolvia
        // getMessage(), que desde que la excepcion lleva clave es la clave.
        disponibilidadService.eliminarHorario(obtenerEmailAutenticado(), id);
        return ResponseEntity.noContent().build();
    }
}