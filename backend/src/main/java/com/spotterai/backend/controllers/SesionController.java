package com.spotterai.backend.controllers;

import com.spotterai.backend.dtos.NuevaSesionDTO;
import com.spotterai.backend.dtos.SesionDTO;
import com.spotterai.backend.dtos.SugerenciaSesionDTO;
import com.spotterai.backend.services.SesionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Quedar para entrenar: proponer, responder y apuntar lo que salió.
 *
 * <p>Los errores se devuelven separados a propósito: 403 cuando la acción no te
 * corresponde (responder por otro, quedar con quien no es tu compañero) y 400
 * cuando lo que pides no tiene sentido (el pasado, una hora imposible). Son dos
 * cosas distintas y la interfaz las trata distinto.
 *
 * <p>Eso ya no se escribe aquí: lo hace {@link ManejadorDeErrores}, que además
 * los redacta en el idioma de la petición. Cada método tenía el mismo par de
 * {@code catch} copiado —seis veces en este fichero— y devolvía el mensaje que
 * la excepción llevara dentro, que estaba en español. Dejarlos ahora sería peor
 * que antes: con la clave dentro de la excepción, un {@code getMessage()} suelto
 * enseñaría «error.sesion.enElPasado» en la pantalla.
 */
@RestController
@RequestMapping("/api/sesiones")
public class SesionController {

    private final SesionService sesionService;

    public SesionController(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    private String emailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /** GET /api/sesiones — lo que sigue contando: por responder, por delante y por apuntar. */
    @GetMapping
    public ResponseEntity<List<SesionDTO>> mias() {
        return ResponseEntity.ok(sesionService.mias(emailAutenticado()));
    }

    /**
     * GET /api/sesiones/pendientes — cuántas esperan tu respuesta.
     *
     * Mismo formato que /api/mensajes/sin-leer: la cabecera suma las tres cosas
     * que esperan algo de ti y no necesita saber nada más de cada una.
     */
    @GetMapping("/pendientes")
    public ResponseEntity<Map<String, Long>> pendientes() {
        return ResponseEntity.ok(Map.of("total", sesionService.pendientesParaMi(emailAutenticado())));
    }

    /**
     * GET /api/sesiones/sugerencia/{otroUsuarioId}
     *
     * Lo que el formulario trae ya puesto, sacado del solape real.
     */
    @GetMapping("/sugerencia/{otroUsuarioId}")
    public ResponseEntity<?> sugerencia(@PathVariable Long otroUsuarioId) {
        return ResponseEntity.ok(sesionService.sugerir(emailAutenticado(), otroUsuarioId));
    }

    /**
     * GET /api/sesiones/con/{otroUsuarioId}
     *
     * La sesión viva con esa persona, para enseñarla dentro del chat. Devuelve
     * 204 cuando no hay ninguna, que no es un error: es lo normal.
     */
    @GetMapping("/con/{otroUsuarioId}")
    public ResponseEntity<?> conMigo(@PathVariable Long otroUsuarioId) {
        return sesionService.conMigo(emailAutenticado(), otroUsuarioId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** POST /api/sesiones/proponer/{otroUsuarioId} */
    @PostMapping("/proponer/{otroUsuarioId}")
    public ResponseEntity<?> proponer(@PathVariable Long otroUsuarioId,
                                      @RequestBody NuevaSesionDTO cuerpo) {
        return ResponseEntity.ok(sesionService.proponer(emailAutenticado(), otroUsuarioId, cuerpo));
    }

    /** POST /api/sesiones/{id}/aceptar */
    @PostMapping("/{id}/aceptar")
    public ResponseEntity<?> aceptar(@PathVariable Long id) {
        return responder(id, true);
    }

    /** POST /api/sesiones/{id}/rechazar */
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable Long id) {
        return responder(id, false);
    }

    private ResponseEntity<?> responder(Long id, boolean acepta) {
        return ResponseEntity.ok(sesionService.responder(emailAutenticado(), id, acepta));
    }

    /** DELETE /api/sesiones/{id} — cancelar, mientras no haya empezado. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        sesionService.cancelar(emailAutenticado(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/sesiones/{id}/confirmar
     *
     * "Sí, entrenamos". Apunta el entrenamiento en tu historial, solo en el tuyo.
     */
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<?> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(sesionService.confirmar(emailAutenticado(), id));
    }
}
