package com.spotterai.backend.controllers;

import com.spotterai.backend.dtos.EntrenamientoDTO;
import com.spotterai.backend.models.Entrenamiento;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.EntrenamientoRepository;
import com.spotterai.backend.services.UsuarioService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/entrenamientos")
public class EntrenamientoController {

    private final EntrenamientoRepository entrenamientoRepository;
    private final UsuarioService usuarioService;

    public EntrenamientoController(EntrenamientoRepository entrenamientoRepository, UsuarioService usuarioService) {
        this.entrenamientoRepository = entrenamientoRepository;
        this.usuarioService = usuarioService;
    }

    // 1. Endpoint para GUARDAR un entrenamiento
    @PostMapping
    public ResponseEntity<?> registrarEntrenamiento(@RequestBody EntrenamientoDTO dto) {
        try {
            String emailLogueado = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioService.buscarPorEmail(emailLogueado)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Entrenamiento nuevo = new Entrenamiento();
            nuevo.setFecha(LocalDate.parse(dto.getFecha()));
            nuevo.setTipo(dto.getTipo());
            nuevo.setDuracionMinutos(dto.getDuracionMinutos());
            nuevo.setLugarONotas(dto.getLugarONotas());
            nuevo.setUsuario(usuario);

            entrenamientoRepository.save(nuevo);
            return ResponseEntity.ok().body("{\"mensaje\": \"Entrenamiento guardado correctamente\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al guardar: " + e.getMessage());
        }
    }

    // 2. Endpoint para OBTENER el historial de entrenamientos
    @GetMapping
    public ResponseEntity<?> obtenerMisEntrenamientos() {
        try {
            String emailLogueado = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioService.buscarPorEmail(emailLogueado)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            List<Entrenamiento> historial = entrenamientoRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId());
            
            // 🔥 LA SOLUCIÓN: Convertimos a DTO para cortar el bucle infinito del Usuario
            List<EntrenamientoDTO> historialDTO = historial.stream().map(ent -> {
                EntrenamientoDTO dto = new EntrenamientoDTO();
                dto.setFecha(ent.getFecha().toString());
                dto.setTipo(ent.getTipo());
                dto.setDuracionMinutos(ent.getDuracionMinutos());
                dto.setLugarONotas(ent.getLugarONotas());
                return dto;
            }).toList();

            return ResponseEntity.ok(historialDTO);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al obtener historial.");
        }
    }
}