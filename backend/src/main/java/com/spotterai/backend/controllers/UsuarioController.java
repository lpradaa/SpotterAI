package com.spotterai.backend.controllers;

import com.spotterai.backend.dtos.UsuarioPerfilDTO;
import com.spotterai.backend.dtos.UsuarioRegistroDTO;
import com.spotterai.backend.dtos.UsuarioResponseDTO;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.services.UsuarioService;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController // Indica que esta clase responderá peticiones web devolviendo JSON
@RequestMapping("/api/usuarios") // Todas las rutas aquí empezarán por esto
@CrossOrigin(origins = "http://localhost:4200") // Permite que tu frontend en Angular se conecte sin bloqueos de seguridad del navegador
public class UsuarioController {

    private final UsuarioService usuarioService;

    // Inyectamos el servicio
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Endpoint de Registro
     * POST http://localhost:8080/api/usuarios/registro
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody UsuarioRegistroDTO dto) {
        try {
            // Le pasamos el DTO al servicio para que haga la magia (validar, cifrar, guardar)
            UsuarioResponseDTO usuarioCreado = usuarioService.registrarUsuario(dto);
            
            // Si todo va bien, devolvemos un 200 OK con los datos seguros (sin contraseña)
            return ResponseEntity.ok(usuarioCreado);
            
        } catch (IllegalArgumentException e) {
            // Si el email ya existía, devolvemos un error 400 Bad Request con el mensaje
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Endpoint para Actualizar Perfil
     * PUT http://localhost:8080/api/usuarios/perfil
     */
    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(@RequestBody UsuarioPerfilDTO dto) {
        try {
            // ¡MAGIA! Sacamos el email directamente del Token JWT del usuario que hace la petición
            String emailLogueado = SecurityContextHolder.getContext().getAuthentication().getName();

            // Actualizamos su perfil de forma segura
            UsuarioResponseDTO usuarioActualizado = usuarioService.actualizarPerfil(emailLogueado, dto);
            
            return ResponseEntity.ok(usuarioActualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error inesperado al actualizar el perfil.");
        }
    }

    /**
     * 🔥 NUEVO: Endpoint para Obtener Mis Datos Reales
     * GET http://localhost:8080/api/usuarios/perfil
     */
    @GetMapping("/perfil")
    public ResponseEntity<?> obtenerMiPerfil() {
        try {
            String emailLogueado = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // Llamamos al nuevo método que nos empaqueta todo, ¡incluyendo los horarios!
            Map<String, Object> perfilCompleto = usuarioService.obtenerMiPerfilCompleto(emailLogueado);
            
            return ResponseEntity.ok(perfilCompleto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al obtener el perfil: " + e.getMessage());
        }
    }

    /**
     * Endpoint para Buscar Compañeros (Matches)
     * GET http://localhost:8080/api/usuarios/matches
     */
    @GetMapping("/matches")
    public ResponseEntity<?> obtenerMatches() {
        try {
            // 1. Sacamos el email del usuario logueado (ej. Carlos) desde su Token
            String emailLogueado = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // 2. Llamamos al servicio para buscar sus matches
            List<UsuarioResponseDTO> posiblesCompañeros = usuarioService.buscarCompañeros(emailLogueado);
            
            // 3. Devolvemos la lista con un 200 OK
            return ResponseEntity.ok(posiblesCompañeros);
            
        } catch (RuntimeException e) {
            // Si no tiene gimnasio u ocurre un error, devolvemos un 400 Bad Request
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al buscar compañeros.");
        }
    }

    /**
     * 🔥 NUEVO: Explorar Comunidad (Modo Tinder)
     * GET http://localhost:8080/api/usuarios/explorar
     */
    @GetMapping("/explorar")
    public ResponseEntity<?> explorarComunidad() {
        try {
            String emailLogueado = SecurityContextHolder.getContext().getAuthentication().getName();
            List<UsuarioResponseDTO> comunidad = usuarioService.explorarComunidad(emailLogueado);
            return ResponseEntity.ok(comunidad);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al cargar la comunidad.");
        }
    }

    
}