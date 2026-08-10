package com.spotterai.backend.seguridad;

import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Borrar la cuenta y todo lo que va con ella.
 *
 * <h2>Por que existe</h2>
 *
 * <p>No habia forma de irse. En la UE el derecho de supresion no es opcional, y
 * aqui pesa mas de lo normal por lo que se guarda: en que gimnasio estas y a que
 * horas. Eso es, dicho claro, donde encontrarte. Una aplicacion que recoge eso y
 * no deja borrarlo no deberia recibir a nadie.
 *
 * <h2>Que se borra, y por que las conversaciones enteras</h2>
 *
 * <p>Todo lo que te incluye: horarios, marcas, entrenamientos, hitos,
 * solicitudes, sesiones, mensajes —los tuyos y los que te escribieron—,
 * bloqueos y reportes en cualquiera de los dos sentidos.
 *
 * <p>Lo ultimo es la decision incomoda, y se toma a conciencia. Habia tres
 * salidas y ninguna es limpia:
 *
 * <ul>
 *   <li><b>Dejar tus mensajes con el autor anonimizado.</b> La conversacion
 *       queda legible para el otro, pero se conserva lo que tu escribiste
 *       despues de que pidieras que te borraran, que es exactamente lo que un
 *       borrado significa que no se haga.
 *   <li><b>Borrar solo los tuyos.</b> Imposible sin mas: cada mensaje apunta a
 *       las dos personas, asi que los que el te mando tambien te referencian.
 *   <li><b>Borrar el hilo entero.</b> El otro pierde tambien lo que escribio el.
 * </ul>
 *
 * <p>Se elige la tercera. Una conversacion de dos es conjunta, y media
 * conversacion llena de huecos no le sirve a nadie; conservar tus palabras
 * contradice el borrado. La pantalla lo dice antes de confirmar, que es lo
 * minimo: quien se va tiene que saber que se lleva eso por delante.
 *
 * <h2>Por que se pide la contraseña</h2>
 *
 * <p>Por lo mismo que para cambiarla: una sesion abierta en un ordenador
 * prestado no deberia bastar para borrarle la cuenta a alguien. Y esto no tiene
 * vuelta atras.
 */
@Service
public class BorradoDeCuenta {

    private static final Logger log = LoggerFactory.getLogger(BorradoDeCuenta.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder cifrador;
    private final DisponibilidadRepository disponibilidades;
    private final EntrenamientoRepository entrenamientos;
    private final HitoRepository hitos;
    private final LevantamientoRepository levantamientos;
    private final MensajeRepository mensajes;
    private final SesionRepository sesiones;
    private final SolicitudRepository solicitudes;
    private final BloqueoRepository bloqueos;
    private final ReporteRepository reportes;

    public BorradoDeCuenta(UsuarioRepository usuarios, PasswordEncoder cifrador,
                           DisponibilidadRepository disponibilidades,
                           EntrenamientoRepository entrenamientos,
                           HitoRepository hitos,
                           LevantamientoRepository levantamientos,
                           MensajeRepository mensajes,
                           SesionRepository sesiones,
                           SolicitudRepository solicitudes,
                           BloqueoRepository bloqueos,
                           ReporteRepository reportes) {
        this.usuarios = usuarios;
        this.cifrador = cifrador;
        this.disponibilidades = disponibilidades;
        this.entrenamientos = entrenamientos;
        this.hitos = hitos;
        this.levantamientos = levantamientos;
        this.mensajes = mensajes;
        this.sesiones = sesiones;
        this.solicitudes = solicitudes;
        this.bloqueos = bloqueos;
        this.reportes = reportes;
    }

    /**
     * Borra la cuenta de quien tenga esa contraseña.
     *
     * <p>Todo en una transaccion: un borrado a medias deja a alguien sin cuenta
     * pero con sus mensajes en la base, que es el peor de los dos mundos.
     *
     * <p>El orden no es arbitrario. Las claves ajenas estan en RESTRICT —a
     * proposito: asi nada se borra por accidente— y eso obliga a vaciar primero
     * todo lo que apunta al usuario. Si aparece una tabla nueva que lo
     * referencie y no se añade aqui, el borrado fallara en vez de dejar basura,
     * que es como tiene que fallar.
     *
     * @return false si la contraseña no es correcta o no existe la cuenta
     */
    @Transactional
    public boolean borrar(String email, String contrasena) {
        Optional<Usuario> encontrado = usuarios.findByEmail(email);
        if (encontrado.isEmpty()) return false;

        Usuario usuario = encontrado.get();
        if (!cifrador.matches(contrasena, usuario.getPassword())) return false;

        Long id = usuario.getId();

        bloqueos.borrarTodosDe(id);
        reportes.borrarTodosDe(id);
        mensajes.borrarTodosDe(id);
        sesiones.borrarTodasDe(id);
        solicitudes.borrarTodasDe(id);
        levantamientos.borrarTodosDe(id);
        entrenamientos.borrarTodosDe(id);
        hitos.borrarTodosDe(id);
        disponibilidades.borrarTodasDe(id);

        usuarios.delete(usuario);

        // Sin el correo ni el nombre: esto queda escrito en el registro del
        // servidor, y borrar una cuenta no puede consistir en copiar quien era a
        // otro sitio.
        log.info("Cuenta borrada (id {}) con todo lo asociado", id);
        return true;
    }
}
