package com.spotterai.backend.controllers;

import com.spotterai.backend.models.Reporte;
import com.spotterai.backend.seguridad.AdminEmails;
import com.spotterai.backend.seguridad.Reportes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Reportar a alguien, y ver lo reportado si eres de los pocos que puede.
 *
 * <p>El GET no usa Spring Security para el filtro de admin —no hay roles
 * definidos en la configuración de seguridad, solo "autenticado" o no—, así
 * que la comprobación se hace aquí, a mano, contra {@link AdminEmails}. Es
 * deliberadamente el único sitio de toda la API que hace esto: mientras siga
 * siendo uno, cambiar cómo se decide quién es admin es tocar una función, no
 * perseguir comprobaciones repartidas por media docena de controladores.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final Reportes reportes;
    private final AdminEmails admins;

    public ReporteController(Reportes reportes, AdminEmails admins) {
        this.reportes = reportes;
        this.admins = admins;
    }

    private String emailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping("/{reportadoId}")
    public ResponseEntity<?> reportar(@PathVariable Long reportadoId,
                                      @RequestBody Map<String, String> cuerpo) {
        reportes.reportar(emailAutenticado(), reportadoId, cuerpo.get("motivo"), cuerpo.get("detalle"));
        return ResponseEntity.noContent().build();
    }

    /**
     * Todos los reportes. Solo para quien está en {@link AdminEmails}.
     *
     * <p>404 y no 403 para quien no es admin: un 403 confirma que el recurso
     * existe y que a ti en concreto no te dejan verlo, que es información de
     * más para cualquiera que no debería estar preguntando esto.
     */
    @GetMapping
    public ResponseEntity<?> todos() {
        if (!admins.esAdmin(emailAutenticado())) {
            return ResponseEntity.notFound().build();
        }

        List<Map<String, Object>> lista = reportes.todos().stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "reportadorNombre", r.getReportador().getNombre(),
                        "reportadorEmail", r.getReportador().getEmail(),
                        "reportadoId", r.getReportado().getId(),
                        "reportadoNombre", r.getReportado().getNombre(),
                        "reportadoEmail", r.getReportado().getEmail(),
                        "motivo", r.getMotivo(),
                        "detalle", r.getDetalle() == null ? "" : r.getDetalle(),
                        "creadoEn", r.getCreadoEn(),
                        "revisado", r.estaRevisado()))
                .toList();

        return ResponseEntity.ok(lista);
    }

    /**
     * Dar un reporte por visto.
     *
     * <p>Mismo 404 que el listado para quien no es admin, y por lo mismo: un 403
     * confirmaria que el recurso existe a alguien que no deberia estar
     * preguntando.
     */
    @PostMapping("/{reporteId}/revisado")
    public ResponseEntity<?> marcarRevisado(@PathVariable Long reporteId) {
        if (!admins.esAdmin(emailAutenticado())) {
            return ResponseEntity.notFound().build();
        }

        reportes.marcarRevisado(reporteId, emailAutenticado());
        return ResponseEntity.noContent().build();
    }
}
