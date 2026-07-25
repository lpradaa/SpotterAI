package com.spotterai.backend.controllers;

import com.spotterai.backend.dtos.SolicitudDTO;
import com.spotterai.backend.services.SolicitudService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
@CrossOrigin(origins = "http://localhost:4200")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    // Método de utilidad para el JWT
    private String obtenerEmailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * 1. ENVIAR SOLICITUD A UN USUARIO
     * POST http://localhost:8080/api/solicitudes/enviar/{receptorId}
     */
    @PostMapping("/enviar/{receptorId}")
    public ResponseEntity<?> enviarSolicitud(@PathVariable Long receptorId) {
        try {
            String email = obtenerEmailAutenticado();
            SolicitudDTO dto = solicitudService.enviarSolicitud(email, receptorId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // Captura si se envía a sí mismo o si ya existe
        }
    }

    /**
     * 2. RESPONDER A UNA SOLICITUD (ACEPTADA o RECHAZADA)
     * PUT http://localhost:8080/api/solicitudes/responder/{solicitudId}?estado=ACEPTADA
     */
    @PutMapping("/responder/{solicitudId}")
    public ResponseEntity<?> responderSolicitud(
            @PathVariable Long solicitudId, 
            @RequestParam String estado) {
        try {
            String email = obtenerEmailAutenticado();
            SolicitudDTO dto = solicitudService.responderSolicitud(email, solicitudId, estado);
            return ResponseEntity.ok(dto);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage()); // 403 Si intenta aceptar una solicitud que no es suya
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 3. VER MIS SOLICITUDES PENDIENTES
     * GET http://localhost:8080/api/solicitudes/pendientes
     */
    @GetMapping("/pendientes")
    public ResponseEntity<List<SolicitudDTO>> obtenerPendientes() {
        String email = obtenerEmailAutenticado();
        return ResponseEntity.ok(solicitudService.obtenerPendientes(email));
    }

    /**
     * 4. VER MIS COMPAÑEROS (SOLICITUDES ACEPTADAS)
     * GET http://localhost:8080/api/solicitudes/aceptadas
     */
    @GetMapping("/aceptadas")
    public ResponseEntity<List<SolicitudDTO>> obtenerAceptadas() {
        String email = obtenerEmailAutenticado();
        return ResponseEntity.ok(solicitudService.obtenerAceptadas(email));
    }

}