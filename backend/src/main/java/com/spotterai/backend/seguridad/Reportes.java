package com.spotterai.backend.seguridad;

import com.spotterai.backend.textos.ErrorDeNegocio;
import com.spotterai.backend.models.Reporte;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.ReporteRepository;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Que la aplicación se entere de que alguien se ha portado mal.
 *
 * <p>Bloquear te protege a ti: dejas de ver a esa persona y ella a ti. No le
 * dice nada a nadie más, así que si la misma persona se comporta igual con
 * cinco personas distintas, cada una de ellas la bloquea por su cuenta y la
 * aplicación nunca junta esos cinco hechos. Reportar es lo que llena ese
 * hueco: un registro de sucesos que alguien —hoy, quien esté en
 * {@link AdminEmails}— puede leer.
 */
@Service
public class Reportes {

    private final ReporteRepository reportes;
    private final UsuarioRepository usuarios;
    private final Clock reloj;

    public Reportes(ReporteRepository reportes, UsuarioRepository usuarios, Clock reloj) {
        this.reportes = reportes;
        this.usuarios = usuarios;
        this.reloj = reloj;
    }

    /**
     * Registra un reporte.
     *
     * <p>No hay restricción de "uno por pareja": son sucesos, no un estado. Y no
     * bloquea automáticamente a quien se reporta —son dos acciones distintas y
     * quien reporta puede querer seguir viendo la conversación, por ejemplo
     * como prueba de lo que está contando—, así que quien quiera las dos cosas
     * las pide las dos.
     */
    @Transactional
    public void reportar(String emailReportador, Long reportadoId, String motivoTexto, String detalle) {
        Usuario reportador = usuarios.findByEmail(emailReportador)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (reportador.getId().equals(reportadoId)) {
            throw ErrorDeNegocio.de("error.reporte.aTiMismo");
        }

        Usuario reportado = usuarios.findById(reportadoId)
                .orElseThrow(() -> new IllegalArgumentException("Esa persona no existe"));

        MotivoReporte motivo = MotivoReporte.desde(motivoTexto)
                .orElseThrow(() -> new IllegalArgumentException("Ese motivo no es válido."));

        // El detalle en blanco se guarda como null, igual que el resto del
        // perfil: "sin detalle" es un solo caso, no una cadena vacía y un null
        // significando lo mismo.
        String detalleLimpio = (detalle == null || detalle.isBlank()) ? null : detalle.trim();

        reportes.save(new Reporte(reportador, reportado, motivo.name(), detalleLimpio,
                LocalDateTime.now(reloj)));
    }

    /** Todos los reportes, más recientes primero. Solo para quien ya se ha comprobado que es admin. */
    @Transactional(readOnly = true)
    public List<Reporte> todos() {
        return reportes.findAllByOrderByCreadoEnDesc();
    }

    /**
     * Dar un reporte por visto.
     *
     * <p>Es lo unico que le faltaba al panel para dejar de ser una lista que
     * crece y nadie sabe por donde iba. No resuelve nada ni sanciona a nadie:
     * eso pasa fuera de la aplicacion, y montar aqui un flujo de sanciones que
     * no existe seria fingir.
     *
     * <p>Quien llama ya ha comprobado que es admin, igual que en {@link #todos()}.
     */
    @Transactional
    public void marcarRevisado(Long reporteId, String porQuien) {
        Reporte reporte = reportes.findById(reporteId)
                .orElseThrow(() -> new IllegalArgumentException("Ese reporte no existe."));

        reporte.marcarRevisado(porQuien, LocalDateTime.now(reloj));
        reportes.save(reporte);
    }
}
