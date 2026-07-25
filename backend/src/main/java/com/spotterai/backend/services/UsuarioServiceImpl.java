package com.spotterai.backend.services;

import com.spotterai.backend.dtos.UsuarioPerfilDTO;
import com.spotterai.backend.dtos.UsuarioRegistroDTO;
import com.spotterai.backend.dtos.UsuarioResponseDTO;
import com.spotterai.backend.matching.CalculadoraCompatibilidad;
import com.spotterai.backend.matching.DiasSemana;
import com.spotterai.backend.matching.ExplicacionMatch;
import com.spotterai.backend.matching.ExplicadorCompatibilidad;
import com.spotterai.backend.matching.PuntuacionCompatibilidad;
import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.DisponibilidadRepository;
import com.spotterai.backend.repositories.GimnasioRepository;
import com.spotterai.backend.repositories.SolicitudRepository;
import com.spotterai.backend.repositories.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
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
    private final DisponibilidadRepository disponibilidadRepository;
    private final ExplicadorCompatibilidad explicador;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                              GimnasioRepository gimnasioRepository, SolicitudRepository solicitudRepository,
                              DisponibilidadRepository disponibilidadRepository,
                              ExplicadorCompatibilidad explicador) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.gimnasioRepository = gimnasioRepository;
        this.solicitudRepository = solicitudRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.explicador = explicador;
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

        if (dto.getGimnasioId() != null) {
            Gimnasio gimnasio = gimnasioRepository.findById(dto.getGimnasioId())
                    .orElseThrow(() -> new IllegalArgumentException("El gimnasio seleccionado no existe."));
            usuario.setGimnasio(gimnasio);
        }

        Usuario guardado = usuarioRepository.save(usuario);

        if (dto.getHorarios() != null) {
            disponibilidadRepository.deleteByUsuarioId(guardado.getId());
            
            for (UsuarioPerfilDTO.HorarioDTO horario : dto.getHorarios()) {
                // Se guarda siempre la forma canonica ("Miércoles", no "miercoles").
                // Si no, el desplegable del perfil no encuentra el valor entre sus
                // opciones, se queda en blanco y al siguiente guardado escribe un
                // dia vacio que ya no cruza con nadie.
                Disponibilidad nuevaDisp = new Disponibilidad(
                    DiasSemana.canonico(horario.getDiaSemana()),
                    LocalTime.parse(horario.getHoraInicio()),
                    LocalTime.parse(horario.getHoraFin()),
                    guardado
                );
                disponibilidadRepository.save(nuevaDisp);
            }
        }

        // 🔥 EXTRAE EL NOMBRE DEL GIMNASIO AQUÍ
        return new UsuarioResponseDTO(
                guardado.getId(), guardado.getNombre(), guardado.getEmail(),
                guardado.getEdad(), guardado.getGenero(), guardado.getPeso(),
                guardado.getNivel(), guardado.getObjetivos(),
                guardado.getGimnasio() != null ? guardado.getGimnasio().getId() : null,
                guardado.getAvatar(), guardado.getBiografia(),
                guardado.getGimnasio() != null ? guardado.getGimnasio().getNombre() : "Gimnasio Habitual"
        );
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

        List<UsuarioPerfilDTO.HorarioDTO> horarios = disponibilidadRepository.findByUsuarioId(usuario.getId())
                .stream().map(d -> {
                    UsuarioPerfilDTO.HorarioDTO h = new UsuarioPerfilDTO.HorarioDTO();
                    h.setDiaSemana(d.getDiaSemana());
                    h.setHoraInicio(d.getHoraInicio().toString()); 
                    h.setHoraFin(d.getHoraFin().toString());
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
        Map<Long, String> estadoPorCompanero = indexarSolicitudes(miUsuario.getId());

        return candidatos.stream()
                .map(candidato -> {
                    PuntuacionCompatibilidad puntuacion = CalculadoraCompatibilidad.calcular(
                            miUsuario, misHorarios,
                            candidato, horariosAjenos.getOrDefault(candidato.getId(), List.of()));
                    return construirDtoDeMatch(candidato, puntuacion,
                            estadoPorCompanero.get(candidato.getId()));
                })
                .sorted(Comparator.comparingInt(UsuarioResponseDTO::getCompatibilidad).reversed())
                .collect(Collectors.toList());
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
                otro, disponibilidadRepository.findByUsuarioId(otro.getId()));

        return explicador.explicar(otro.getNombre(), puntuacion);
    }

    /** Una sola consulta para los horarios de todos los candidatos, agrupados por usuario. */
    private Map<Long, List<Disponibilidad>> cargarHorariosDe(List<Usuario> candidatos) {
        List<Long> ids = candidatos.stream().map(Usuario::getId).toList();
        return disponibilidadRepository.findByUsuarioIdIn(ids).stream()
                .filter(d -> d.getUsuario() != null)
                .collect(Collectors.groupingBy(d -> d.getUsuario().getId()));
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
        dto.setResumenCompatibilidad(puntuacion.factorDominante().detalle());
        dto.setDiasEnComun(puntuacion.solape().dias());
        dto.setMinutosEnComun(puntuacion.solape().minutosSemanales());

        // Los flags eran codigo muerto: la query anterior ya excluia a quien tuviera
        // solicitud, asi que nunca podian ser true. Ahora los candidatos vienen todos
        // y estos valores deciden si la tarjeta ofrece "Conectar" o "Chatear".
        dto.setYaConectado("ACEPTADA".equals(estadoSolicitud));
        dto.setSolicitudPendiente("PENDIENTE".equals(estadoSolicitud));

        return dto;
    }

    /** Mapeo comun de entidad a DTO, para no repetir el constructor de 12 argumentos. */
    private UsuarioResponseDTO aDTO(Usuario u) {
        return new UsuarioResponseDTO(
                u.getId(), u.getNombre(), u.getEmail(),
                u.getEdad(), u.getGenero(), u.getPeso(),
                u.getNivel(), u.getObjetivos(),
                u.getGimnasio() != null ? u.getGimnasio().getId() : null,
                u.getAvatar(), u.getBiografia(),
                u.getGimnasio() != null ? u.getGimnasio().getNombre() : "Gimnasio Habitual");
    }

    @Override
    public List<UsuarioResponseDTO> explorarComunidad(String email) {
        Usuario miUsuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        List<Usuario> todosMenosYo = usuarioRepository.findByIdNot(miUsuario.getId());

        return todosMenosYo.stream().filter(u -> {
            boolean hayIda = solicitudRepository.findFirstByEmisorIdAndReceptorId(miUsuario.getId(), u.getId()).isPresent();
            boolean hayVuelta = solicitudRepository.findFirstByEmisorIdAndReceptorId(u.getId(), miUsuario.getId()).isPresent();
            
            return !hayIda && !hayVuelta;
            
        }).map(u -> new UsuarioResponseDTO(
            u.getId(), u.getNombre(), u.getEmail(), 
            u.getEdad(), u.getGenero(), u.getPeso(), 
            u.getNivel(), u.getObjetivos(), 
            u.getGimnasio() != null ? u.getGimnasio().getId() : null,
            u.getAvatar(), u.getBiografia(),
            u.getGimnasio() != null ? u.getGimnasio().getNombre() : "Gimnasio Habitual" // 🔥 EXTRAE EL NOMBRE DEL GIMNASIO AQUÍ
        )).collect(Collectors.toList());
    }
}