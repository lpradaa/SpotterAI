package com.spotterai.backend.services;

import com.spotterai.backend.dtos.UsuarioPerfilDTO;
import com.spotterai.backend.dtos.UsuarioRegistroDTO;
import com.spotterai.backend.dtos.UsuarioResponseDTO;
import com.spotterai.backend.matching.CalculadoraCompatibilidad;
import com.spotterai.backend.matching.CalculadoraFuerza;
import com.spotterai.backend.matching.Ejercicio;
import com.spotterai.backend.matching.DiasSemana;
import com.spotterai.backend.matching.ExplicacionMatch;
import com.spotterai.backend.matching.FactorCompatibilidad;
import com.spotterai.backend.matching.ExplicadorCompatibilidad;
import com.spotterai.backend.matching.PuntuacionCompatibilidad;
import com.spotterai.backend.matching.RendimientoDelPerfil;
import com.spotterai.backend.matching.Rutina;
import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import java.time.LocalDate;

import com.spotterai.backend.dtos.HitoDTO;
import com.spotterai.backend.dtos.LevantamientoDTO;
import com.spotterai.backend.dtos.PerfilPublicoDTO;
import com.spotterai.backend.models.Hito;
import com.spotterai.backend.models.Levantamiento;
import com.spotterai.backend.repositories.DisponibilidadRepository;
import com.spotterai.backend.repositories.EntrenamientoRepository;
import com.spotterai.backend.repositories.HitoRepository;
import com.spotterai.backend.repositories.LevantamientoRepository;
import com.spotterai.backend.repositories.GimnasioRepository;
import com.spotterai.backend.repositories.SesionRepository;
import com.spotterai.backend.repositories.SolicitudRepository;
import com.spotterai.backend.repositories.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service 
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final GimnasioRepository gimnasioRepository;
    private final SolicitudRepository solicitudRepository;
    /**
     * Maximo de franjas que un usuario puede marcar como habituales.
     *
     * <p>Sin tope, todo el mundo las marcaria todas y el campo dejaria de informar,
     * igual que unos deslizadores que todos ponen al maximo. Tres obliga a elegir
     * cuales son de verdad las anclas de la semana.
     */
    static final int MAX_FRANJAS_HABITUALES = 3;

    /** Coincide con la columna. Se corta en el servidor y no solo en el formulario. */
    static final int MAX_BIOGRAFIA = 280;

    private final DisponibilidadRepository disponibilidadRepository;
    private final ExplicadorCompatibilidad explicador;
    private final HitoRepository hitoRepository;
    private final EntrenamientoRepository entrenamientoRepository;
    private final LevantamientoRepository levantamientoRepository;

    /**
     * Para poder decir cuantas veces habeis quedado ya.
     *
     * Es la señal que de verdad separa a un compañero de un candidato: uno con
     * el que ya has ido tres veces no se parece en nada a uno con el 90 % de
     * compatibilidad y ninguna sesion.
     */
    private final SesionRepository sesionRepository;

    /** Inyectado para que las pruebas puedan decidir que dia es hoy. */
    private final Clock reloj;

    /**
     * Tope de marcas por persona.
     *
     * Tres bastan para saber si podeis cubriros: los basicos. Sin tope, el
     * perfil se convierte en una hoja de calculo y la comparacion se diluye
     * entre ejercicios accesorios que no dicen nada de la pareja.
     */
    static final int MAX_LEVANTAMIENTOS = 3;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                              GimnasioRepository gimnasioRepository, SolicitudRepository solicitudRepository,
                              DisponibilidadRepository disponibilidadRepository,
                              ExplicadorCompatibilidad explicador,
                              HitoRepository hitoRepository,
                              EntrenamientoRepository entrenamientoRepository,
                              LevantamientoRepository levantamientoRepository,
                              SesionRepository sesionRepository,
                              Clock reloj) {
        this.levantamientoRepository = levantamientoRepository;
        this.sesionRepository = sesionRepository;
        this.reloj = reloj;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.gimnasioRepository = gimnasioRepository;
        this.solicitudRepository = solicitudRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.explicador = explicador;
        this.hitoRepository = hitoRepository;
        this.entrenamientoRepository = entrenamientoRepository;
    }

    @Override
    public UsuarioResponseDTO registrarUsuario(UsuarioRegistroDTO dto) {
        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Error: El email ya está registrado en SpotterAI.");
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(dto.getNombre());
        nuevoUsuario.setEmail(dto.getEmail());
        nuevoUsuario.setEdad(dto.getEdad());
        nuevoUsuario.setGenero(dto.getGenero());
        nuevoUsuario.setPeso(dto.getPeso());
        nuevoUsuario.setPassword(passwordEncoder.encode(dto.getPassword()));

        Usuario guardado = usuarioRepository.save(nuevoUsuario);

        return new UsuarioResponseDTO(guardado.getId(), guardado.getNombre(), guardado.getEmail(), guardado.getEdad(), guardado.getGenero(), guardado.getPeso());
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Override
    public List<Usuario> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll();
    }

    @Override
    @Transactional
    public UsuarioResponseDTO actualizarPerfil(String email, UsuarioPerfilDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado en la base de datos."));

        usuario.setEdad(dto.getEdad());
        usuario.setGenero(dto.getGenero());
        usuario.setPeso(dto.getPeso());
        usuario.setNivel(dto.getNivel());
        usuario.setObjetivos(dto.getObjetivos());
        usuario.setAvatar(dto.getAvatar());
        usuario.setBiografia(recortar(dto.getBiografia(), MAX_BIOGRAFIA));

        // Se valida contra el enum antes de guardar: la columna es texto libre y
        // sin esto cualquier cadena entraria en la base para leerse luego como
        // "sin rutina", que es un dato perdido sin que nadie se entere.
        usuario.setRutina(Rutina.desde(dto.getRutina()).map(Enum::name).orElse(null));

        // Solo se acepta una ruta servida por nosotros. Sin esta comprobacion, el
        // campo seria un hueco para meter la URL de cualquier sitio y hacer que
        // todos los navegadores que vieran el perfil la pidieran.
        if (dto.getFotoUrl() == null || dto.getFotoUrl().isBlank()) {
            usuario.setFotoUrl(null);
        } else if (dto.getFotoUrl().startsWith("/api/medios/")) {
            usuario.setFotoUrl(dto.getFotoUrl());
        }

        // Solo si llega: una pantalla que edite el perfil sin tocar la meta no
        // deberia borrarla por omision.
        if (dto.getMetaSemanal() != null) {
            usuario.setMetaSemanal(Math.clamp(dto.getMetaSemanal(), 1, 14));
        }

        if (dto.getGimnasioId() != null) {
            Gimnasio gimnasio = gimnasioRepository.findById(dto.getGimnasioId())
                    .orElseThrow(() -> new IllegalArgumentException("El gimnasio seleccionado no existe."));
            usuario.setGimnasio(gimnasio);
        }

        Usuario guardado = usuarioRepository.save(usuario);

        // Null significa "no los toques": una pantalla que edite otra parte del
        // perfil no debe borrar las marcas por no mandarlas.
        if (dto.getLevantamientos() != null) {
            guardarLevantamientos(guardado, dto.getLevantamientos());
        }

        if (dto.getHorarios() != null) {
            disponibilidadRepository.deleteByUsuarioId(guardado.getId());

            // Tope de franjas habituales: si se pudiera marcar todo, el campo dejaria
            // de distinguir nada y volveriamos a tener un unico nivel de compromiso.
            // Obligar a elegir es lo que hace que la señal informe.
            int habitualesUsadas = 0;

            for (UsuarioPerfilDTO.HorarioDTO horario : dto.getHorarios()) {
                boolean habitual = horario.isHabitual() && habitualesUsadas < MAX_FRANJAS_HABITUALES;
                if (habitual) habitualesUsadas++;

                // Se guarda siempre la forma canonica ("Miércoles", no "miercoles").
                // Si no, el desplegable del perfil no encuentra el valor entre sus
                // opciones, se queda en blanco y al siguiente guardado escribe un
                // dia vacio que ya no cruza con nadie.
                Disponibilidad nuevaDisp = new Disponibilidad(
                    DiasSemana.canonico(horario.getDiaSemana()),
                    LocalTime.parse(horario.getHoraInicio()),
                    LocalTime.parse(horario.getHoraFin()),
                    guardado,
                    habitual
                );
                disponibilidadRepository.save(nuevaDisp);
            }
        }

        // Por el mapeador comun y no a mano: esta copia repetida del constructor
        // es justo la razon de que la foto y la rutina se guardaran bien pero no
        // volvieran en la respuesta del guardado.
        return aDTO(guardado);
    }

    /**
     * Recorta un texto libre al maximo de su columna.
     *
     * El formulario ya limita, pero el formulario no es la unica via de entrada:
     * sin esto, una peticion directa con 5000 caracteres reventaria al guardar.
     * En blanco se guarda como null para que "sin biografia" sea un solo caso.
     */
    private static String recortar(String texto, int maximo) {
        if (texto == null) return null;
        String limpio = texto.trim();
        if (limpio.isEmpty()) return null;
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }

    @Override
    public Map<String, Object> obtenerMiPerfilCompleto(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Map<String, Object> perfil = new HashMap<>();
        perfil.put("id", usuario.getId());
        perfil.put("nombre", usuario.getNombre());
        perfil.put("email", usuario.getEmail());
        perfil.put("edad", usuario.getEdad());
        perfil.put("genero", usuario.getGenero());
        perfil.put("peso", usuario.getPeso());
        perfil.put("nivel", usuario.getNivel());
        perfil.put("objetivos", usuario.getObjetivos());
        perfil.put("gimnasioId", usuario.getGimnasio() != null ? usuario.getGimnasio().getId() : null);
        perfil.put("avatar", usuario.getAvatar());
        // Sin esta linea la foto se guardaba pero no volvia, asi que al recargar
        // la pantalla desaparecia. Es el tercer campo al que le pasa lo mismo:
        // este mapa se construye a mano y añadir una columna no obliga a nada.
        perfil.put("fotoUrl", usuario.getFotoUrl());
        perfil.put("biografia", usuario.getBiografia());
        perfil.put("rutina", Rutina.desde(usuario.getRutina()).map(Enum::name).orElse(null));
        perfil.put("rutinasDisponibles", Arrays.stream(Rutina.values())
                .map(r -> Map.of("clave", r.name(), "nombre", r.getNombre())).toList());

        List<Levantamiento> misLevantamientos = levantamientoRepository.findByUsuarioId(usuario.getId());
        perfil.put("levantamientos", misLevantamientos.stream()
                .map(UsuarioServiceImpl::aLevantamientoDTO).toList());
        // El catalogo viaja con el perfil para que el desplegable no tenga que
        // llevar una copia de la lista, que es como acaban divergiendo.
        perfil.put("ejerciciosDisponibles", Arrays.stream(Ejercicio.values())
                .map(e -> Map.of("clave", e.name(), "nombre", e.getNombre())).toList());
        perfil.put("metaSemanal", usuario.getMetaSemanal() != null ? usuario.getMetaSemanal() : 4);

        // Viaja con el perfil y no en un endpoint aparte: se calcula de los
        // mismos datos que ya estamos leyendo, y quien pinta el perfil es quien
        // necesita saber que le falta.
        List<Disponibilidad> misHorarios = disponibilidadRepository.findByUsuarioId(usuario.getId());
        perfil.put("rendimiento", RendimientoDelPerfil.de(
                usuario, !misHorarios.isEmpty(), !misLevantamientos.isEmpty()));

        List<UsuarioPerfilDTO.HorarioDTO> horarios = disponibilidadRepository.findByUsuarioId(usuario.getId())
                .stream().map(d -> {
                    UsuarioPerfilDTO.HorarioDTO h = new UsuarioPerfilDTO.HorarioDTO();
                    h.setDiaSemana(d.getDiaSemana());
                    h.setHoraInicio(d.getHoraInicio().toString());
                    h.setHoraFin(d.getHoraFin().toString());
                    h.setHabitual(d.isHabitual());
                    return h;
                }).collect(Collectors.toList());

        perfil.put("horarios", horarios);

        return perfil;
    }

    /**
     * Puntua a todos los candidatos y los devuelve ordenados de mayor a menor
     * compatibilidad.
     *
     * <p>Sustituye al filtro binario original (mismo gimnasio Y mismo nivel), que
     * descartaba de golpe a cualquiera que no encajara exactamente e ignoraba por
     * completo los horarios. Ahora el gimnasio y el nivel son factores que suman,
     * no requisitos que excluyen, asi que un usuario sin gimnasio ya no revienta
     * la pantalla: simplemente pierde esos puntos.
     *
     * <p>Cuatro consultas fijas, independientemente del numero de candidatos.
     */
    @Override
    public List<UsuarioResponseDTO> buscarCompañeros(String email) {
        Usuario miUsuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<Usuario> candidatos = usuarioRepository.findByIdNot(miUsuario.getId());
        if (candidatos.isEmpty()) return List.of();

        List<Disponibilidad> misHorarios = disponibilidadRepository.findByUsuarioId(miUsuario.getId());
        Map<Long, List<Disponibilidad>> horariosAjenos = cargarHorariosDe(candidatos);
        Map<Long, List<Levantamiento>> levantamientosAjenos = cargarLevantamientosDe(candidatos);
        List<Levantamiento> misLevantamientos = levantamientoRepository.findByUsuarioId(miUsuario.getId());
        Map<Long, String> estadoPorCompanero = indexarSolicitudes(miUsuario.getId());

        return candidatos.stream()
                .map(candidato -> {
                    PuntuacionCompatibilidad puntuacion = CalculadoraCompatibilidad.calcular(
                            miUsuario, misHorarios, misLevantamientos,
                            candidato, horariosAjenos.getOrDefault(candidato.getId(), List.of()),
                            levantamientosAjenos.getOrDefault(candidato.getId(), List.of()));
                    UsuarioResponseDTO dto = construirDtoDeMatch(candidato, puntuacion,
                            estadoPorCompanero.get(candidato.getId()));

                    // De la misma comparacion que ha puntuado, no de otra hecha
                    // aparte: si se recalculara, la lista podria decir que os
                    // cubris mientras la explicacion dice lo contrario.
                    dto.setFuerzaCompatible(CalculadoraFuerza.comparar(
                                    misLevantamientos,
                                    levantamientosAjenos.getOrDefault(candidato.getId(), List.of()))
                            .podeisCubriros().orElse(null));
                    return dto;
                })
                .sorted(Comparator.comparingInt(UsuarioResponseDTO::getCompatibilidad).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public PerfilPublicoDTO verPerfilDe(String email, Long otroUsuarioId) {
        Usuario yo = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Usuario otro = usuarioRepository.findById(otroUsuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Esa persona no existe."));

        if (yo.getId().equals(otro.getId())) {
            throw new IllegalArgumentException("Tu propio perfil se edita, no se visita.");
        }

        PuntuacionCompatibilidad puntuacion = CalculadoraCompatibilidad.calcular(
                yo, disponibilidadRepository.findByUsuarioId(yo.getId()),
                levantamientoRepository.findByUsuarioId(yo.getId()),
                otro, disponibilidadRepository.findByUsuarioId(otro.getId()),
                levantamientoRepository.findByUsuarioId(otro.getId()));

        String estado = indexarSolicitudes(yo.getId()).get(otro.getId());

        List<HitoDTO> hitos = hitoRepository.findByUsuarioIdOrderByFechaDescIdDesc(otro.getId())
                .stream().map(UsuarioServiceImpl::aHitoDTO).toList();

        // Ventana de siete dias hacia atras y no "esta semana": el lunes por la
        // mañana todo el mundo apareceria inactivo aunque hubiera entrenado el
        // domingo.
        long entrenos = entrenamientoRepository.countByUsuarioIdAndFechaGreaterThanEqual(
                otro.getId(), LocalDate.now().minusDays(7));

        return new PerfilPublicoDTO(
                otro.getId(),
                otro.getNombre(),
                otro.getAvatar(),
                otro.getFotoUrl(),
                otro.getBiografia(),
                otro.getNivel(),
                otro.getObjetivos(),
                Rutina.desde(otro.getRutina()).map(Rutina::getNombre).orElse(null),
                otro.getEdad(),
                otro.getGimnasio() != null ? otro.getGimnasio().getNombre() : null,
                puntuacion.total(),
                puntuacion.etiqueta(),
                resumenDe(puntuacion),
                puntuacion.solape().franjas(),
                hitos,
                levantamientoRepository.findByUsuarioId(otro.getId()).stream()
                        .map(UsuarioServiceImpl::aLevantamientoDTO).toList(),
                entrenos,
                sesionRepository.contarQuedadasEntre(yo.getId(), otro.getId(),
                        LocalDate.now(reloj), LocalTime.now(reloj)),
                "ACEPTADA".equals(estado),
                "PENDIENTE".equals(estado));
    }

    /**
     * Reemplaza las marcas de alguien por las que llegan del formulario.
     *
     * Se borra y se reinserta, como con los horarios: intentar casar cada fila
     * con la que le corresponde complica el codigo para ahorrar tres inserts.
     */
    private void guardarLevantamientos(Usuario usuario, List<UsuarioPerfilDTO.LevantamientoEntradaDTO> entradas) {
        levantamientoRepository.deleteByUsuarioId(usuario.getId());

        // El flush no es decorativo: dentro de una transaccion, Hibernate agrupa
        // las operaciones por tipo y manda todos los INSERT antes que los DELETE.
        // Sin forzarlo aqui, guardar un peso nuevo para un ejercicio que ya tenias
        // choca con la restriccion unica (usuario, ejercicio) y devuelve un 500.
        //
        // Los horarios usan este mismo patron de borrar y reinsertar sin flush, y
        // ahi no se nota justamente porque no tienen restriccion unica.
        levantamientoRepository.flush();

        Set<Ejercicio> yaPuestos = new HashSet<>();

        for (UsuarioPerfilDTO.LevantamientoEntradaDTO entrada : entradas) {
            if (yaPuestos.size() >= MAX_LEVANTAMIENTOS) break;

            Optional<Ejercicio> ejercicio = Ejercicio.desde(entrada.getEjercicio());
            // Un ejercicio desconocido, sin peso o sin repeticiones se ignora en
            // vez de reventar: es un formulario a medio rellenar, no un ataque.
            if (ejercicio.isEmpty() || entrada.getPeso() == null || entrada.getRepeticiones() == null) continue;
            if (entrada.getPeso() <= 0 || entrada.getRepeticiones() <= 0) continue;

            // La restriccion unica no admite el mismo ejercicio dos veces, y
            // llegar hasta la base para enterarse seria un 500 evitable.
            if (!yaPuestos.add(ejercicio.get())) continue;

            Levantamiento l = new Levantamiento();
            l.setUsuario(usuario);
            l.setEjercicio(ejercicio.get());
            // Topes de cordura: el record del mundo en peso muerto no llega a 505 kg
            l.setPeso(Math.min(entrada.getPeso(), 600));
            l.setRepeticiones(Math.min(entrada.getRepeticiones(), CalculadoraFuerza.MAX_REPETICIONES));

            levantamientoRepository.save(l);
        }
    }

    private static LevantamientoDTO aLevantamientoDTO(Levantamiento l) {
        return new LevantamientoDTO(
                l.getEjercicio().name(), l.getEjercicio().getNombre(),
                l.getPeso(), l.getRepeticiones());
    }

    /** La razon principal por la que encajais, en una frase. */
    private static String resumenDe(PuntuacionCompatibilidad puntuacion) {
        FactorCompatibilidad dominante = puntuacion.factorDominante();
        return dominante != null ? dominante.detalle() : "Perfil sin datos suficientes";
    }

    static HitoDTO aHitoDTO(Hito h) {
        return new HitoDTO(h.getId(), h.getTitulo(), h.getDescripcion(),
                h.getFecha(), h.getMedioUrl(), h.getMedioTipo());
    }

    @Override
    public ExplicacionMatch explicarMatch(String email, Long otroUsuarioId) {
        Usuario miUsuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Usuario otro = usuarioRepository.findById(otroUsuarioId)
                .orElseThrow(() -> new IllegalArgumentException("El usuario sugerido no existe"));

        if (miUsuario.getId().equals(otro.getId())) {
            throw new IllegalArgumentException("No puedes pedir la compatibilidad contigo mismo.");
        }

        PuntuacionCompatibilidad puntuacion = CalculadoraCompatibilidad.calcular(
                miUsuario, disponibilidadRepository.findByUsuarioId(miUsuario.getId()),
                levantamientoRepository.findByUsuarioId(miUsuario.getId()),
                otro, disponibilidadRepository.findByUsuarioId(otro.getId()),
                levantamientoRepository.findByUsuarioId(otro.getId()));

        return explicador.explicar(otro.getNombre(), puntuacion);
    }

    /** Una sola consulta para los horarios de todos los candidatos, agrupados por usuario. */
    private Map<Long, List<Disponibilidad>> cargarHorariosDe(List<Usuario> candidatos) {
        List<Long> ids = candidatos.stream().map(Usuario::getId).toList();
        return disponibilidadRepository.findByUsuarioIdIn(ids).stream()
                .filter(d -> d.getUsuario() != null)
                .collect(Collectors.groupingBy(d -> d.getUsuario().getId()));
    }

    /** En una consulta para todo el grupo, igual que los horarios. */
    private Map<Long, List<Levantamiento>> cargarLevantamientosDe(List<Usuario> candidatos) {
        List<Long> ids = candidatos.stream().map(Usuario::getId).toList();
        if (ids.isEmpty()) return Map.of();

        return levantamientoRepository.findByUsuarioIdIn(ids).stream()
                .filter(l -> l.getUsuario() != null)
                .collect(Collectors.groupingBy(l -> l.getUsuario().getId()));
    }

    /**
     * Indexa en memoria el estado de la solicitud frente a cada companero, en
     * cualquiera de las dos direcciones. Antes esto costaba dos consultas por
     * candidato.
     */
    private Map<Long, String> indexarSolicitudes(Long miId) {
        Map<Long, String> porCompanero = new HashMap<>();
        for (Solicitud s : solicitudRepository.findTodasPorUsuario(miId)) {
            Long idCompanero = s.getEmisor().getId().equals(miId)
                    ? s.getReceptor().getId()
                    : s.getEmisor().getId();
            // Si hubiera varias entre los mismos usuarios, ACEPTADA manda sobre el resto
            porCompanero.merge(idCompanero, s.getEstado(),
                    (previo, nuevo) -> "ACEPTADA".equals(previo) ? previo : nuevo);
        }
        return porCompanero;
    }

    private UsuarioResponseDTO construirDtoDeMatch(Usuario u, PuntuacionCompatibilidad puntuacion, String estadoSolicitud) {
        UsuarioResponseDTO dto = aDTO(u);

        dto.setCompatibilidad(puntuacion.total());
        dto.setEtiquetaCompatibilidad(puntuacion.etiqueta());
        FactorCompatibilidad dominante = puntuacion.factorDominante();
        dto.setResumenCompatibilidad(dominante != null ? dominante.detalle() : "Perfil sin datos suficientes");
        dto.setDiasEnComun(puntuacion.solape().dias());
        dto.setMinutosEnComun(puntuacion.solape().minutosSemanales());
        dto.setDiasFijosEnComun(puntuacion.solape().diasAncla());
        dto.setCompatibilidadIncompleta(!puntuacion.esCompleta());
        dto.setFranjasEnComun(puntuacion.solape().franjas().stream()
                .map(f -> new UsuarioResponseDTO.FranjaComunDTO(
                        f.dia(), f.inicio().toString(), f.fin().toString(), f.ambosFijos()))
                .toList());

        // Los flags eran codigo muerto: la query anterior ya excluia a quien tuviera
        // solicitud, asi que nunca podian ser true. Ahora los candidatos vienen todos
        // y estos valores deciden si la tarjeta ofrece "Conectar" o "Chatear".
        dto.setYaConectado("ACEPTADA".equals(estadoSolicitud));
        dto.setSolicitudPendiente("PENDIENTE".equals(estadoSolicitud));

        return dto;
    }

    /** Mapeo comun de entidad a DTO, para no repetir el constructor de 12 argumentos. */
    private UsuarioResponseDTO aDTO(Usuario u) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO(
                u.getId(), u.getNombre(), u.getEmail(),
                u.getEdad(), u.getGenero(), u.getPeso(),
                u.getNivel(), u.getObjetivos(),
                u.getGimnasio() != null ? u.getGimnasio().getId() : null,
                u.getAvatar(), u.getBiografia(),
                u.getGimnasio() != null ? u.getGimnasio().getNombre() : "Gimnasio Habitual");

        // Fuera del constructor porque ese ya tiene doce parametros posicionales
        // y añadir el trece es pedir que alguien los cruce al llamarlo.
        dto.setFotoUrl(u.getFotoUrl());
        dto.setRutina(Rutina.desde(u.getRutina()).map(Rutina::getNombre).orElse(null));
        return dto;
    }

    /**
     * La comunidad entera, con quien todavia no hay ninguna solicitud.
     *
     * <p>Devuelve exactamente la misma informacion que las sugerencias: puntuacion,
     * franjas en comun y desglose. Antes era una lista sin puntuar, de modo que la
     * pantalla de explorar mostraba perfiles sueltos y no habia forma de saber si
     * merecia la pena escribir a alguien. Un solo camino de calculo para las dos
     * vistas: el tablero enseña la cabeza de la lista y explorar deja filtrarla.
     */
    @Override
    public List<UsuarioResponseDTO> explorarComunidad(String email) {
        Usuario miUsuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Map<Long, String> estadoPorCompanero = indexarSolicitudes(miUsuario.getId());

        List<Usuario> candidatos = usuarioRepository.findByIdNot(miUsuario.getId()).stream()
                .filter(u -> !estadoPorCompanero.containsKey(u.getId()))
                .toList();

        if (candidatos.isEmpty()) return List.of();

        List<Disponibilidad> misHorarios = disponibilidadRepository.findByUsuarioId(miUsuario.getId());
        Map<Long, List<Disponibilidad>> horariosAjenos = cargarHorariosDe(candidatos);

        // Los levantamientos tambien. Esto llamaba a la version de cuatro
        // argumentos, la que calcula sin fuerza, asi que la misma persona salia
        // con un porcentaje aqui y otro distinto en el tablero: sin marcas, el
        // factor de fuerza queda "sin datos" y entra el descuento por evidencia.
        // Dos numeros para la misma pareja es peor que un numero malo, porque el
        // que mira no sabe cual creer.
        Map<Long, List<Levantamiento>> levantamientosAjenos = cargarLevantamientosDe(candidatos);
        List<Levantamiento> misLevantamientos = levantamientoRepository.findByUsuarioId(miUsuario.getId());

        return candidatos.stream()
                .map(candidato -> {
                    List<Levantamiento> suyos =
                            levantamientosAjenos.getOrDefault(candidato.getId(), List.of());

                    UsuarioResponseDTO dto = construirDtoDeMatch(
                            candidato,
                            CalculadoraCompatibilidad.calcular(
                                    miUsuario, misHorarios, misLevantamientos,
                                    candidato, horariosAjenos.getOrDefault(candidato.getId(), List.of()),
                                    suyos),
                            null);

                    dto.setFuerzaCompatible(CalculadoraFuerza.comparar(misLevantamientos, suyos)
                            .podeisCubriros().orElse(null));
                    return dto;
                })
                .sorted(Comparator.comparingInt(UsuarioResponseDTO::getCompatibilidad).reversed())
                .collect(Collectors.toList());
    }
}