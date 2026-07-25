package com.spotterai.backend.controllers;

import com.spotterai.backend.dtos.MensajeDTO;
import com.spotterai.backend.services.MensajeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mensajes")
@CrossOrigin(origins = "http://localhost:4200")
public class MensajeController {

    private final MensajeService mensajeService;

    public MensajeController(MensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    /**
     * POST /api/mensajes/enviar/{receptorId}
     * Body: "¡Hola! ¿A qué hora vamos al gimnasio?"
     */
    @PostMapping("/enviar/{receptorId}")
    public ResponseEntity<?> enviarMensaje(@PathVariable Long receptorId, @RequestBody String contenido) {
        try {
            String emailEmisor = SecurityContextHolder.getContext().getAuthentication().getName();
            String contenidoLimpio = contenido.replace("\"", "").trim(); // Limpiamos comillas del JSON

            MensajeDTO mensajeEnviado = mensajeService.enviarMensaje(emailEmisor, receptorId, contenidoLimpio);
            return ResponseEntity.ok(mensajeEnviado);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al enviar el mensaje.");
        }
    }

    /**
     * GET /api/mensajes/historial/{otroUsuarioId}
     */
    @GetMapping("/historial/{otroUsuarioId}")
    public ResponseEntity<?> obtenerHistorial(@PathVariable Long otroUsuarioId) {
        try {
            String emailLogueado = SecurityContextHolder.getContext().getAuthentication().getName();
            List<MensajeDTO> historial = mensajeService.obtenerHistorial(emailLogueado, otroUsuarioId);
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al recuperar el historial.");
        }
    }
}