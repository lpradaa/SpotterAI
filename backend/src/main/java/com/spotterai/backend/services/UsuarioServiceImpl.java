package com.spotterai.backend.services;

import com.spotterai.backend.dtos.UsuarioPerfilDTO;
import com.spotterai.backend.dtos.UsuarioRegistroDTO;
import com.spotterai.backend.dtos.UsuarioResponseDTO;
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

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, GimnasioRepository gimnasioRepository, SolicitudRepository solicitudRepository, DisponibilidadRepository disponibilidadRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.gimnasioRepository = gimnasioRepository;
        this.solicitudRepository = solicitudRepository;
        this.disponibilidadRepository = disponibilidadRepository;
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
                Disponibilidad nuevaDisp = new Disponibilidad(
                    horario.getDiaSemana(),
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

    @Override
    public List<UsuarioResponseDTO> buscarCompañeros(String email) {
        Usuario miUsuario = usuarioRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        List<Usuario> matches = usuarioRepository.buscarMatches(miUsuario.getGimnasio().getId(), miUsuario.getId(), miUsuario.getNivel());

        return matches.stream().map(u -> {
            // 🔥 EXTRAE EL NOMBRE DEL GIMNASIO AQUÍ
            UsuarioResponseDTO dto = new UsuarioResponseDTO(
                u.getId(), u.getNombre(), u.getEmail(), 
                u.getEdad(), u.getGenero(), u.getPeso(), 
                u.getNivel(), u.getObjetivos(), 
                u.getGimnasio() != null ? u.getGimnasio().getId() : null,
                u.getAvatar(), u.getBiografia(),
                u.getGimnasio() != null ? u.getGimnasio().getNombre() : "Gimnasio Habitual"
            );
            
            Optional<Solicitud> ida = solicitudRepository.findFirstByEmisorIdAndReceptorId(miUsuario.getId(), u.getId());
            Optional<Solicitud> vuelta = solicitudRepository.findFirstByEmisorIdAndReceptorId(u.getId(), miUsuario.getId());
            Solicitud sol = ida.orElse(vuelta.orElse(null));
            
            if (sol != null) {
                dto.setYaConectado("ACEPTADA".equals(sol.getEstado()));
                dto.setSolicitudPendiente("PENDIENTE".equals(sol.getEstado()));
            }
            return dto;
        }).collect(Collectors.toList());
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