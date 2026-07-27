package com.spotterai.backend.services;

import com.spotterai.backend.dtos.NuevaSesionDTO;
import com.spotterai.backend.dtos.SesionDTO;
import com.spotterai.backend.dtos.SugerenciaSesionDTO;
import com.spotterai.backend.eventos.CanalEventos;
import com.spotterai.backend.eventos.TipoEvento;
import com.spotterai.backend.matching.CalculadoraSolape;
import com.spotterai.backend.matching.ProximaOcasion;
import com.spotterai.backend.matching.SolapeHorario;
import com.spotterai.backend.models.Entrenamiento;
import com.spotterai.backend.models.Sesion;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.DisponibilidadRepository;
import com.spotterai.backend.repositories.EntrenamientoRepository;
import com.spotterai.backend.repositories.SesionRepository;
import com.spotterai.backend.repositories.SolicitudRepository;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
public class SesionServiceImpl implements SesionService {

    /** Nadie queda a un año vista, y una fecha asi suele ser un dedazo. */
    static final int DIAS_MAXIMOS_POR_DELANTE = 120;

    /** Lo que dura una sesion como maximo. Por encima es un error de tecleo. */
    static final int MINUTOS_MAXIMOS = 4 * 60;

    static final int MINUTOS_MINIMOS = 15;

    static final int MAX_NOTA = 140;

    private final SesionRepository sesionRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudRepository solicitudRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final EntrenamientoRepository entrenamientoRepository;
    private final CanalEventos canalEventos;

    /**
     * Inyectado y no {@code LocalDateTime.now()} suelto por el codigo: media
     * clase decide cosas comparando con "ahora", y sin poder mover ese ahora las
     * pruebas tendrian que elegir entre esperar a que llegue el lunes o no
     * comprobar nada.
     */
    private final Clock reloj;

    public SesionServiceImpl(SesionRepository sesionRepository,
                             UsuarioRepository usuarioRepository,
                             SolicitudRepository solicitudRepository,
                             DisponibilidadRepository disponibilidadRepository,
                             EntrenamientoRepository entrenamientoRepository,
                             CanalEventos canalEventos,
                             Clock reloj) {
        this.sesionRepository = sesionRepository;
        this.usuarioRepository = usuarioRepository;
        this.solicitudRepository = solicitudRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.entrenamientoRepository = entrenamientoRepository;
        this.canalEventos = canalEventos;
        this.reloj = reloj;
    }

    @Override
    public SugerenciaSesionDTO sugerir(String email, Long otroUsuarioId) {
        Usuario yo = porEmail(email);
        Usuario otro = porId(otroUsuarioId);
        exigirCompaneros(yo, otro);

        SolapeHorario solape = CalculadoraSolape.calcular(
                disponibilidadRepository.findByUsuarioId(yo.getId()),
                disponibilidadRepository.findByUsuarioId(otro.getId()));

        String donde = dondeEntrenariais(yo, otro);

        return ProximaOcasion.primera(solape.franjas(), ahora())
                .map(ocasion -> new SugerenciaSesionDTO(true, ocasion.fecha(),
                        ocasion.inicio(), ocasion.fin(), ocasion.ambosFijos(), donde))
                .orElseGet(() -> SugerenciaSesionDTO.sinFranjas(donde));
    }

    @Override
    @Transactional
    public SesionDTO proponer(String email, Long otroUsuarioId, NuevaSesionDTO datos) {
        Usuario yo = porEmail(email);
        Usuario otro = porId(otroUsuarioId);
        exigirCompaneros(yo, otro);

        // Una sola propuesta viva por par. No es un dato imposible —la base no lo
        // impide— pero dos propuestas a la vez entre los mismos dos convierten el
        // chat en un acertijo sobre a cual se esta contestando.
        if (sesionRepository.propuestaViva(yo.getId(), otro.getId()).isPresent()) {
            throw new IllegalArgumentException(
                    "Ya hay una propuesta en marcha con esta persona. Resolvedla antes de hacer otra.");
        }

        LocalDate fecha = aFecha(datos.fecha());
        LocalTime inicio = aHora(datos.horaInicio(), "de inicio");
        LocalTime fin = aHora(datos.horaFin(), "de fin");

        LocalDateTime comienzo = LocalDateTime.of(fecha, inicio);
        if (!comienzo.isAfter(ahora())) {
            throw new IllegalArgumentException("No se puede quedar en el pasado.");
        }
        if (fecha.isAfter(ahora().toLocalDate().plusDays(DIAS_MAXIMOS_POR_DELANTE))) {
            throw new IllegalArgumentException("Esa fecha queda demasiado lejos.");
        }

        int duracion = (int) java.time.Duration.between(inicio, fin).toMinutes();
        if (duracion < MINUTOS_MINIMOS) {
            throw new IllegalArgumentException("La sesión tiene que durar al menos un cuarto de hora.");
        }
        if (duracion > MINUTOS_MAXIMOS) {
            throw new IllegalArgumentException("Esa sesión dura demasiado, revisa las horas.");
        }

        Sesion sesion = new Sesion();
        sesion.setProponente(yo);
        sesion.setInvitado(otro);
        sesion.setFecha(fecha);
        sesion.setHoraInicio(inicio);
        sesion.setHoraFin(fin);
        sesion.setNota(recortar(datos.nota()));
        sesion.setCreadaEn(ahora());

        // El gimnasio no se pregunta: si los dos entrenan en el mismo, ya se sabe;
        // y si no coinciden, ponerle uno seria decidir por ellos.
        if (yo.getGimnasio() != null && otro.getGimnasio() != null
                && yo.getGimnasio().getId().equals(otro.getGimnasio().getId())) {
            sesion.setGimnasio(yo.getGimnasio());
        }

        Sesion guardada = sesionRepository.save(sesion);

        canalEventos.publicar(otro.getId(), TipoEvento.SESION, aDTO(guardada, otro.getId()));

        return aDTO(guardada, yo.getId());
    }

    @Override
    @Transactional
    public SesionDTO responder(String email, Long sesionId, boolean acepta) {
        Usuario yo = porEmail(email);
        Sesion sesion = porIdDeSesion(sesionId);

        if (!sesion.getInvitado().getId().equals(yo.getId())) {
            throw new SecurityException("Solo puede responder la persona invitada.");
        }
        if (!sesion.estaPendiente()) {
            throw new IllegalArgumentException("Esta propuesta ya está resuelta.");
        }
        // Aceptar una hora que ya ha pasado deja en el calendario un plan que
        // nadie puede cumplir. Se cierra sola en vez de quedarse ahi.
        if (!sesion.comienzo().isAfter(ahora())) {
            sesion.setEstado(Sesion.CANCELADA);
            sesion.setRespondidaEn(ahora());
            sesionRepository.save(sesion);
            throw new IllegalArgumentException("Esa hora ya ha pasado. Proponed otra.");
        }

        sesion.setEstado(acepta ? Sesion.ACEPTADA : Sesion.RECHAZADA);
        sesion.setRespondidaEn(ahora());
        Sesion guardada = sesionRepository.save(sesion);

        Long proponenteId = guardada.getProponente().getId();
        canalEventos.publicar(proponenteId, TipoEvento.SESION_RESPONDIDA, aDTO(guardada, proponenteId));

        return aDTO(guardada, yo.getId());
    }

    @Override
    @Transactional
    public void cancelar(String email, Long sesionId) {
        Usuario yo = porEmail(email);
        Sesion sesion = porIdDeSesion(sesionId);

        if (!sesion.participa(yo.getId())) {
            throw new SecurityException("Esa sesión no es tuya.");
        }
        if (!sesion.estaPendiente() && !sesion.estaEnMarcha(ahora())) {
            throw new IllegalArgumentException("Esta sesión ya no se puede cancelar.");
        }

        sesion.setEstado(Sesion.CANCELADA);
        sesion.setRespondidaEn(ahora());
        sesionRepository.save(sesion);

        Long otroId = sesion.elOtro(yo.getId()).getId();
        canalEventos.publicar(otroId, TipoEvento.SESION_RESPONDIDA, aDTO(sesion, otroId));
    }

    @Override
    @Transactional
    public SesionDTO confirmar(String email, Long sesionId) {
        Usuario yo = porEmail(email);
        Sesion sesion = porIdDeSesion(sesionId);

        if (!sesion.participa(yo.getId())) {
            throw new SecurityException("Esa sesión no es tuya.");
        }
        if (!sesion.yaOcurrio(ahora())) {
            throw new IllegalArgumentException("Esa sesión todavía no ha ocurrido.");
        }
        if (sesion.confirmadaPor(yo.getId())) {
            throw new IllegalArgumentException("Ya la habías apuntado.");
        }

        sesion.confirmarPor(yo.getId());
        Sesion guardada = sesionRepository.save(sesion);

        // Aqui es donde el bucle se cierra: lo que empezo como un porcentaje
        // acaba siendo una fila del historial de entrenamientos, que es lo que
        // mueve la barra de la semana y lo que ve quien visita el perfil.
        Usuario companero = sesion.elOtro(yo.getId());
        Entrenamiento entrenamiento = new Entrenamiento();
        entrenamiento.setUsuario(yo);
        entrenamiento.setFecha(sesion.getFecha());
        entrenamiento.setTipo("Sesión con " + companero.getNombre());
        entrenamiento.setDuracionMinutos(
                (int) java.time.Duration.between(sesion.getHoraInicio(), sesion.getHoraFin()).toMinutes());
        entrenamiento.setLugarONotas(sesion.getGimnasio() != null
                ? sesion.getGimnasio().getNombre() : null);
        entrenamientoRepository.save(entrenamiento);

        return aDTO(guardada, yo.getId());
    }

    @Override
    public List<SesionDTO> mias(String email) {
        Usuario yo = porEmail(email);
        return sesionRepository.vivasDe(yo.getId()).stream()
                .filter(s -> sigueContando(s, yo.getId()))
                .map(s -> aDTO(s, yo.getId()))
                .toList();
    }

    /**
     * Si esta sesion todavia espera algo de esta persona.
     *
     * <p>No basta con que este en PROPUESTA o ACEPTADA: una aceptada que ya paso
     * y que ya has apuntado no espera nada, y seguia apareciendo en el tablero
     * anunciando en futuro un entrenamiento de la semana pasada. Una propuesta a
     * la que se le paso la hora tampoco: ya no se puede aceptar.
     */
    private boolean sigueContando(Sesion sesion, Long quienMira) {
        LocalDateTime ahora = ahora();
        if (sesion.estaPendiente()) return sesion.comienzo().isAfter(ahora);
        return sesion.estaEnMarcha(ahora) || !sesion.confirmadaPor(quienMira);
    }

    @Override
    public Optional<SesionDTO> conMigo(String email, Long otroUsuarioId) {
        Usuario yo = porEmail(email);

        // La que importa dentro del chat es una: la pendiente si la hay, y si no
        // la aceptada mas proxima. Las resueltas no pintan nada ahi.
        return sesionRepository.entreLosDos(yo.getId(), otroUsuarioId).stream()
                .filter(s -> (s.estaPendiente() || Sesion.ACEPTADA.equals(s.getEstado()))
                        && sigueContando(s, yo.getId()))
                .min((a, b) -> {
                    if (a.estaPendiente() != b.estaPendiente()) return a.estaPendiente() ? -1 : 1;
                    return a.comienzo().compareTo(b.comienzo());
                })
                .map(s -> aDTO(s, yo.getId()));
    }

    // --- Apoyo ---

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    private Usuario porEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private Usuario porId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Esa persona no existe."));
    }

    private Sesion porIdDeSesion(Long id) {
        return sesionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Esa sesión no existe."));
    }

    /**
     * Quedar es un paso mas que escribirse, asi que exige lo mismo: ser
     * compañeros. Sin esto, cualquiera podria llenarle el calendario a
     * desconocidos.
     */
    private void exigirCompaneros(Usuario yo, Usuario otro) {
        if (yo.getId().equals(otro.getId())) {
            throw new IllegalArgumentException("No puedes quedar contigo mismo.");
        }

        List<Solicitud> relaciones = solicitudRepository.findAceptadasPorUsuario(yo.getId());
        boolean sonCompaneros = relaciones.stream().anyMatch(s ->
                s.getEmisor().getId().equals(otro.getId()) || s.getReceptor().getId().equals(otro.getId()));

        if (!sonCompaneros) {
            throw new SecurityException("Solo puedes quedar con tus compañeros.");
        }
    }

    private String dondeEntrenariais(Usuario yo, Usuario otro) {
        if (yo.getGimnasio() != null && otro.getGimnasio() != null
                && yo.getGimnasio().getId().equals(otro.getGimnasio().getId())) {
            return yo.getGimnasio().getNombre();
        }
        return null;
    }

    private static LocalDate aFecha(String valor) {
        try {
            return LocalDate.parse(valor);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Esa fecha no vale.");
        }
    }

    private static LocalTime aHora(String valor, String cual) {
        try {
            return LocalTime.parse(valor);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("La hora " + cual + " no vale.");
        }
    }

    private static String recortar(String nota) {
        if (nota == null) return null;
        String limpia = nota.trim();
        if (limpia.isEmpty()) return null;
        return limpia.length() <= MAX_NOTA ? limpia : limpia.substring(0, MAX_NOTA);
    }

    private SesionDTO aDTO(Sesion sesion, Long quienMira) {
        Usuario otro = sesion.elOtro(quienMira);
        boolean mia = sesion.getProponente().getId().equals(quienMira);
        LocalDateTime ahora = ahora();

        return new SesionDTO(
                sesion.getId(),
                otro.getId(),
                otro.getNombre(),
                otro.getAvatar(),
                otro.getFotoUrl(),
                sesion.getFecha(),
                sesion.getHoraInicio(),
                sesion.getHoraFin(),
                sesion.getGimnasio() != null ? sesion.getGimnasio().getNombre() : null,
                sesion.getEstado(),
                sesion.getNota(),
                mia,
                sesion.estaPendiente() && !mia && sesion.comienzo().isAfter(ahora),
                sesion.estaPendiente() || sesion.estaEnMarcha(ahora),
                sesion.yaOcurrio(ahora) && !sesion.confirmadaPor(quienMira),
                sesion.confirmadaPor(quienMira));
    }
}
