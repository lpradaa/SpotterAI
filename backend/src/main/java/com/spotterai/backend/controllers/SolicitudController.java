package com.spotterai.backend.controllers;

import com.spotterai.backend.dtos.SolicitudDTO;
import com.spotterai.backend.services.SolicitudService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
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
        // Enviarse una solicitud a uno mismo, que ya exista o que haya un
        // bloqueo de por medio los para el servicio con su clave, y de eso
        // responde ManejadorDeErrores.
        return ResponseEntity.ok(
                solicitudService.enviarSolicitud(obtenerEmailAutenticado(), receptorId));
    }

    /**
     * 2. RESPONDER A UNA SOLICITUD (ACEPTADA o RECHAZADA)
     * PUT http://localhost:8080/api/solicitudes/responder/{solicitudId}?estado=ACEPTADA
     */
    @PutMapping("/responder/{solicitudId}")
    public ResponseEntity<?> responderSolicitud(
            @PathVariable Long solicitudId, 
            @RequestParam String estado) {
        return ResponseEntity.ok(
                solicitudService.responderSolicitud(obtenerEmailAutenticado(), solicitudId, estado));
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

    /**
     * 5. DESHACER LA RELACIÓN CON ALGUIEN
     * DELETE http://localhost:8080/api/solicitudes/con/{otroUsuarioId}
     *
     * Retira una solicitud que enviaste o deja de ser compañero. Sin esto, y con
     * la restricción única en la base, enviar una solicitud por error bloqueaba
     * ese par para siempre.
     */
    @DeleteMapping("/con/{otroUsuarioId}")
    public ResponseEntity<?> deshacerRelacion(@PathVariable Long otroUsuarioId) {
        solicitudService.deshacerRelacion(obtenerEmailAutenticado(), otroUsuarioId);
        return ResponseEntity.noContent().build();
    }

}