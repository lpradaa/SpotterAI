package com.spotterai.backend.controllers;

import com.spotterai.backend.dtos.ConversacionDTO;
import com.spotterai.backend.dtos.MensajeDTO;
import com.spotterai.backend.dtos.NuevoMensajeDTO;
import com.spotterai.backend.services.MensajeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mensajes")
public class MensajeController {

    private final MensajeService mensajeService;

    public MensajeController(MensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    private String emailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * POST /api/mensajes/enviar/{receptorId}
     * Body: {"contenido": "¿A qué hora vamos al gimnasio?"}
     */
    @PostMapping("/enviar/{receptorId}")
    public ResponseEntity<?> enviarMensaje(@PathVariable Long receptorId,
                                           @RequestBody NuevoMensajeDTO cuerpo) {
        String contenido = cuerpo == null || cuerpo.contenido() == null ? "" : cuerpo.contenido().trim();
        if (contenido.isEmpty()) {
            return ResponseEntity.badRequest().body("El mensaje está vacío.");
        }

        try {
            MensajeDTO mensajeEnviado = mensajeService.enviarMensaje(emailAutenticado(), receptorId, contenido);
            return ResponseEntity.ok(mensajeEnviado);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al enviar el mensaje.");
        }
    }

    /**
     * GET /api/mensajes/historial/{otroUsuarioId}
     *
     * Marca de paso la conversación como leída: abrirla es leerla.
     */
    @GetMapping("/historial/{otroUsuarioId}")
    public ResponseEntity<?> obtenerHistorial(@PathVariable Long otroUsuarioId) {
        try {
            List<MensajeDTO> historial = mensajeService.obtenerHistorial(emailAutenticado(), otroUsuarioId);
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al recuperar el historial.");
        }
    }

    /**
     * GET /api/mensajes/conversaciones
     *
     * Los compañeros con su último mensaje y lo que queda sin leer, ordenados
     * por actividad. Una sola petición para pintar la lista entera.
     */
    @GetMapping("/conversaciones")
    public ResponseEntity<?> listarConversaciones() {
        try {
            List<ConversacionDTO> conversaciones = mensajeService.listarConversaciones(emailAutenticado());
            return ResponseEntity.ok(conversaciones);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al recuperar las conversaciones.");
        }
    }

    /**
     * POST /api/mensajes/leidos/{otroUsuarioId}
     *
     * Para cuando llega un mensaje con esa conversación ya abierta: el servidor
     * no puede saber que lo estás mirando, así que hay que decírselo. Abrir la
     * conversación por primera vez ya lo hace solo, desde el historial.
     */
    @PostMapping("/leidos/{otroUsuarioId}")
    public ResponseEntity<?> marcarLeida(@PathVariable Long otroUsuarioId) {
        mensajeService.marcarConversacionLeida(emailAutenticado(), otroUsuarioId);
        return ResponseEntity.noContent().build();
    }

    /** GET /api/mensajes/sin-leer — total, para el indicador de la cabecera. */
    @GetMapping("/sin-leer")
    public ResponseEntity<?> contarSinLeer() {
        return ResponseEntity.ok(Map.of("total", mensajeService.contarSinLeer(emailAutenticado())));
    }
}
