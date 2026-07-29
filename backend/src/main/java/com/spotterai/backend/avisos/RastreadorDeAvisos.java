package com.spotterai.backend.avisos;

import com.spotterai.backend.models.Sesion;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.repositories.SesionRepository;
import com.spotterai.backend.repositories.SolicitudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Avisa por correo de lo que sigue esperando una respuesta.
 *
 * <h2>Por que un barrido y no un correo al crear la solicitud</h2>
 *
 * <p>Mandarlo en caliente, dentro de la peticion que crea la solicitud, tiene
 * tres problemas y ninguno es teorico:
 *
 * <ul>
 *   <li><b>Le escribe a quien ya lo ha visto.</b> Si la otra persona tiene la
 *       aplicacion abierta, el aviso le ha llegado por SSE y esta mirandolo. Un
 *       correo por algo que ya tiene delante es exactamente lo que hace que la
 *       gente silencie los correos de una aplicacion, y entonces se pierden
 *       tambien los que importaban.
 *   <li><b>Ata el SMTP a la peticion.</b> Un servidor de correo lento convierte
 *       "aceptar" en tres segundos de espera, y uno caido lo convierte en un
 *       error de una accion que en realidad ha funcionado.
 *   <li><b>No se puede reintentar.</b> Si el envio falla, no hay nada que
 *       recuerde que ese aviso quedo pendiente.
 * </ul>
 *
 * <p>Asi que se espera {@link #DEMORA} y despues se mira que sigue sin
 * responder. Lo que se contesto en ese rato no genera correo, y no hace falta
 * preguntarle a nadie si estaba conectado: <b>que siga en PENDIENTE es la
 * prueba de que no se ha visto o no se ha atendido</b>, que es justo el caso en
 * el que un correo sirve para algo.
 *
 * <h2>Por que hay un limite de antiguedad</h2>
 *
 * <p>{@link #CADUCIDAD} evita dos cosas. La primera es avisar hoy de una
 * solicitud de hace dos semanas —que es lo que pasaria al desplegar esto por
 * primera vez sobre una base con historia, y le llegaria una tanda de correos a
 * todo el mundo a la vez—. La segunda es reintentar para siempre contra una
 * direccion que no existe.
 *
 * <h2>Que impide mandarlo dos veces</h2>
 *
 * <p>La marca {@code avisadoEn} en la propia fila, y se pone <b>despues</b> de
 * que el envio haya salido bien. Si el correo falla, la fila se queda sin marca
 * y el siguiente barrido lo reintenta; si el proceso se reinicia a medias, la
 * marca de los que si salieron ya esta en la base.
 */
@Component
public class RastreadorDeAvisos {

    private static final Logger log = LoggerFactory.getLogger(RastreadorDeAvisos.class);

    /**
     * Lo que se espera antes de escribir.
     *
     * <p>Suficiente para que quien estaba conectado responda sin recibir un
     * correo inutil, y poco para que el aviso siga sirviendo: una propuesta para
     * mañana no aguanta una espera de horas.
     */
    static final Duration DEMORA = Duration.ofMinutes(10);

    /** Mas viejo que esto ya no se avisa: ni al desplegar, ni reintentando. */
    static final Duration CADUCIDAD = Duration.ofDays(2);

    private final SolicitudRepository solicitudes;
    private final SesionRepository sesiones;
    private final RedactorDeAvisos redactor;
    private final Cartero cartero;
    private final Clock reloj;

    public RastreadorDeAvisos(SolicitudRepository solicitudes, SesionRepository sesiones,
                              RedactorDeAvisos redactor, Cartero cartero, Clock reloj) {
        this.solicitudes = solicitudes;
        this.sesiones = sesiones;
        this.redactor = redactor;
        this.cartero = cartero;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "${spotterai.correo.cada-cuanto-ms:60000}")
    @Transactional
    public void barrer() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime hasta = ahora.minus(DEMORA);
        LocalDateTime desde = ahora.minus(CADUCIDAD);

        for (Solicitud s : solicitudes.pendientesPorAvisar(desde, hasta)) {
            intentar(() -> redactor.paraSolicitud(s), () -> s.setAvisadoEn(ahora),
                    "solicitud " + s.getId());
        }

        // Solo las de un dia que aun no ha pasado: avisar de una propuesta para
        // el martes cuando ya es miercoles no le sirve a nadie.
        for (Sesion s : sesiones.propuestasPorAvisar(desde, hasta, LocalDate.now(reloj))) {
            intentar(() -> redactor.paraSesion(s), () -> s.setAvisadoEn(ahora),
                    "sesion " + s.getId());
        }
    }

    /**
     * Manda uno y marca solo si ha salido.
     *
     * <p>Un fallo no puede tumbar el barrido entero: si la segunda direccion de
     * la tanda rebota, las quince siguientes tienen que salir igual. Y no se
     * marca, para que el siguiente barrido lo reintente hasta que caduque.
     */
    private void intentar(java.util.function.Supplier<Aviso> redactar, Runnable marcar, String que) {
        try {
            cartero.enviar(redactar.get());
            marcar.run();
        } catch (Exception e) {
            log.warn("No se ha podido avisar de {}: {}", que, e.getMessage());
        }
    }
}
