package com.spotterai.backend.seguridad;

import com.spotterai.backend.textos.ErrorDeNegocio;
import com.spotterai.backend.models.Bloqueo;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.BloqueoRepository;
import com.spotterai.backend.repositories.SolicitudRepository;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Quitarte a alguien de encima.
 *
 * <h2>Por que esto es lo que faltaba</h2>
 *
 * <p>Esta aplicacion le enseña a un desconocido en que gimnasio entrenas y a que
 * horas exactas. Es el dato mas sensible que guarda, y es el que convierte una
 * molestia en algo fisico: quien sabe eso sabe donde encontrarte.
 *
 * <p>Hasta ahora no habia forma de cortar. "Deshacer relacion" solo borraba la
 * fila de la solicitud, asi que la otra persona podia mandarte otra al segundo
 * siguiente, y mientras tanto te seguia viendo en Explorar con tu horario
 * delante.
 *
 * <h2>Bloquear y no denunciar</h2>
 *
 * <p>Una denuncia necesita a alguien que la lea. Aqui no hay nadie, y un boton
 * de denunciar sin moderacion detras es teatro —uno peligroso, porque quien lo
 * pulsa se queda creyendo que ha hecho algo—. Bloquear funciona con cero
 * personas al otro lado: lo aplica la propia aplicacion, al momento.
 *
 * <h2>Es silencioso</h2>
 *
 * <p>Al bloqueado no se le avisa de nada. Decirselo es lo que convierte un
 * bloqueo en un motivo: quien se entera de que le has bloqueado sabe que le has
 * bloqueado, y esa es justo la persona de la que uno se queria librar. Desde su
 * lado, simplemente dejas de aparecer.
 */
@Service
public class Bloqueos {

    private final BloqueoRepository bloqueos;
    private final UsuarioRepository usuarios;
    private final SolicitudRepository solicitudes;
    private final Clock reloj;

    public Bloqueos(BloqueoRepository bloqueos, UsuarioRepository usuarios,
                    SolicitudRepository solicitudes, Clock reloj) {
        this.bloqueos = bloqueos;
        this.usuarios = usuarios;
        this.solicitudes = solicitudes;
        this.reloj = reloj;
    }

    /**
     * Bloquea, y de paso deshace lo que hubiera.
     *
     * <p>Lo segundo no es un extra: si la relacion siguiera en pie, el bloqueado
     * conservaria el chat abierto y podria seguir escribiendo. Bloquear a alguien
     * que puede seguir escribiendote no es bloquear.
     *
     * <p>Es idempotente. Pulsar dos veces —o dos pestañas a la vez— no puede dar
     * error: quien esta bloqueando a alguien quiere que quede bloqueado, y un
     * mensaje de "ya lo estaba" solo estorba.
     */
    @Transactional
    public void bloquear(String email, Long otroId) {
        Usuario yo = usuarios.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (yo.getId().equals(otroId)) {
            throw ErrorDeNegocio.de("error.bloqueo.aTiMismo");
        }

        Usuario otro = usuarios.findById(otroId)
                .orElseThrow(() -> new IllegalArgumentException("Esa persona no existe"));

        if (bloqueos.findByBloqueadorIdAndBloqueadoId(yo.getId(), otroId).isEmpty()) {
            bloqueos.save(new Bloqueo(yo, otro, LocalDateTime.now(reloj)));
        }

        // La relacion, en cualquier direccion y en cualquier estado: aceptada,
        // pendiente o rechazada. Mientras exista, el otro tiene chat.
        solicitudes.findFirstByEmisorIdAndReceptorId(yo.getId(), otroId)
                .or(() -> solicitudes.findFirstByEmisorIdAndReceptorId(otroId, yo.getId()))
                .ifPresent(solicitudes::delete);
    }

    /**
     * Quita tu bloqueo.
     *
     * <p>No devuelve la relacion que habia: eso lo decidis vosotros otra vez.
     * Restaurarla sola pondria a alguien a hablar con quien bloqueo sin haberlo
     * pedido.
     */
    @Transactional
    public void desbloquear(String email, Long otroId) {
        Usuario yo = usuarios.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        bloqueos.findByBloqueadorIdAndBloqueadoId(yo.getId(), otroId)
                .ifPresent(bloqueos::delete);
    }

    /** A quien has bloqueado tú, que son los únicos que puedes desbloquear. */
    @Transactional(readOnly = true)
    public List<Bloqueo> mios(String email) {
        return usuarios.findByEmail(email)
                .map(yo -> bloqueos.findByBloqueadorIdOrderByCreadoEnDesc(yo.getId()))
                .orElse(List.of());
    }
}
