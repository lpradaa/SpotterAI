package com.spotterai.backend.services;

import com.spotterai.backend.dtos.SolicitudDTO;
import com.spotterai.backend.eventos.CanalEventos;
import com.spotterai.backend.eventos.TipoEvento;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.SolicitudRepository;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SolicitudServiceImpl implements SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final CanalEventos canalEventos;

    public SolicitudServiceImpl(SolicitudRepository solicitudRepository, UsuarioRepository usuarioRepository,
                                CanalEventos canalEventos) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.canalEventos = canalEventos;
    }

    @Override
    public Solicitud crearSolicitud(Solicitud solicitud) {
        solicitud.setEstado("PENDIENTE");
        return solicitudRepository.save(solicitud);
    }

    @Override
    public Solicitud cambiarEstado(Long id, String nuevoEstado) {
        Solicitud solicitud = solicitudRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        
        solicitud.setEstado(nuevoEstado);
        return solicitudRepository.save(solicitud);
    }

    @Override
    public SolicitudDTO enviarSolicitud(String emailEmisor, Long receptorId) {
        Usuario emisor = usuarioRepository.findByEmail(emailEmisor)
                .orElseThrow(() -> new IllegalArgumentException("Emisor no encontrado"));
        
        Usuario receptor = usuarioRepository.findById(receptorId)
                .orElseThrow(() -> new IllegalArgumentException("Receptor no encontrado"));

        if (emisor.getId().equals(receptor.getId())) {
            throw new IllegalArgumentException("No puedes enviarte una solicitud a ti mismo.");
        }

        // Usamos findFirstBy para evitar errores si en el futuro hay basura en la BD
        Optional<Solicitud> previaIda = solicitudRepository.findFirstByEmisorIdAndReceptorId(emisor.getId(), receptor.getId());
        Optional<Solicitud> previaVuelta = solicitudRepository.findFirstByEmisorIdAndReceptorId(receptor.getId(), emisor.getId());
        
        if (previaIda.isPresent() || previaVuelta.isPresent()) {
            throw new IllegalArgumentException("Ya existe una solicitud entre estos usuarios.");
        }

        Solicitud nueva = new Solicitud();
        nueva.setEmisor(emisor);
        nueva.setReceptor(receptor);
        nueva.setEstado("PENDIENTE"); 

        Solicitud guardada = solicitudRepository.save(nueva);
        SolicitudDTO dto = mapearADTO(guardada);

        // Esto es lo que de verdad se perdía sin tiempo real: una solicitud sin
        // responder no se veía hasta recargar, y mientras tanto se pierde un
        // compañero.
        canalEventos.publicar(receptor.getId(), TipoEvento.SOLICITUD, dto);

        return dto;
    }

    @Override
    public SolicitudDTO responderSolicitud(String emailReceptor, Long solicitudId, String nuevoEstado) {
        Usuario usuarioLogueado = usuarioRepository.findByEmail(emailReceptor)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!solicitud.getReceptor().getId().equals(usuarioLogueado.getId())) {
            throw new SecurityException("No tienes permiso para responder a esta solicitud.");
        }

        if (!nuevoEstado.equals("ACEPTADA") && !nuevoEstado.equals("RECHAZADA")) {
            throw new IllegalArgumentException("Estado no válido.");
        }

        solicitud.setEstado(nuevoEstado);
        Solicitud actualizada = solicitudRepository.save(solicitud);
        SolicitudDTO dto = mapearADTO(actualizada);

        // Al emisor, que es quien está esperando respuesta.
        canalEventos.publicar(solicitud.getEmisor().getId(), TipoEvento.SOLICITUD_RESPONDIDA, dto);

        return dto;
    }

    @Override
    public List<SolicitudDTO> obtenerPendientes(String emailReceptor) {
        Usuario receptor = usuarioRepository.findByEmail(emailReceptor)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<Solicitud> pendientes = solicitudRepository.findByReceptorIdAndEstado(receptor.getId(), "PENDIENTE");
        
        return pendientes.stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    private SolicitudDTO mapearADTO(Solicitud s) {
        return new SolicitudDTO(
                s.getId(),
                s.getEmisor().getId(),
                s.getEmisor().getNombre(),
                s.getReceptor().getId(),
                s.getReceptor().getNombre(),
                s.getEstado()
        );
    }

    @Override
    public List<SolicitudDTO> obtenerAceptadas(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con email: " + email));

        List<Solicitud> solicitudesAceptadas = solicitudRepository.findAceptadasPorUsuario(usuario.getId());

        // 🔥 LA SOLUCIÓN AL MISTERIO DE LOS CLONES:
        // Usamos un Set (conjunto) para ir recordando a qué compañeros hemos procesado ya.
        Set<Long> idsCompañerosVistos = new HashSet<>();
        List<SolicitudDTO> listaSinDuplicados = new ArrayList<>();

        for (Solicitud s : solicitudesAceptadas) {
            // 1. Averiguamos quién es "la otra persona" en esta solicitud (para no añadirnos a nosotros mismos)
            Long idCompañero = s.getEmisor().getId().equals(usuario.getId()) 
                    ? s.getReceptor().getId() 
                    : s.getEmisor().getId();

            // 2. Si es la primera vez que vemos a este compañero en el bucle, lo añadimos
            if (!idsCompañerosVistos.contains(idCompañero)) {
                idsCompañerosVistos.add(idCompañero); // Lo apuntamos en la libreta para no repetirlo
                
                // Lo convertimos a DTO y lo metemos en la lista final
                listaSinDuplicados.add(new SolicitudDTO(
                        s.getId(),
                        s.getEmisor().getId(),
                        s.getEmisor().getNombre(),
                        s.getReceptor().getId(),
                        s.getReceptor().getNombre(),
                        s.getEstado()
                ));
            }
        }

        return listaSinDuplicados;
    }
}