package com.spotterai.backend.services;

import com.spotterai.backend.dtos.ConversacionDTO;
import com.spotterai.backend.dtos.MensajeDTO;
import com.spotterai.backend.dtos.SinLeerPorEmisor;
import com.spotterai.backend.eventos.CanalEventos;
import com.spotterai.backend.eventos.TipoEvento;
import com.spotterai.backend.models.Mensaje;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.repositories.MensajeRepository;
import com.spotterai.backend.repositories.UsuarioRepository;
import com.spotterai.backend.repositories.SolicitudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MensajeServiceImpl implements MensajeService {

    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudRepository solicitudRepository; // Necesario para comprobar el Match
    private final CanalEventos canalEventos;

    public MensajeServiceImpl(MensajeRepository mensajeRepository, UsuarioRepository usuarioRepository,
                              SolicitudRepository solicitudRepository, CanalEventos canalEventos) {
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.solicitudRepository = solicitudRepository;
        this.canalEventos = canalEventos;
    }

    @Override
    public MensajeDTO enviarMensaje(String emailEmisor, Long receptorId, String contenido) {
        Usuario emisor = usuarioRepository.findByEmail(emailEmisor)
                .orElseThrow(() -> new IllegalArgumentException("Emisor no encontrado"));
                
        Usuario receptor = usuarioRepository.findById(receptorId)
                .orElseThrow(() -> new IllegalArgumentException("Receptor no encontrado"));

        // REGLA CLAVE: Comprobar si tienen un Match aceptado
        List<Solicitud> match = solicitudRepository.findByEmisorIdAndEstadoOrReceptorIdAndEstado(
                emisor.getId(), "ACEPTADA", emisor.getId(), "ACEPTADA"
        );
        
        boolean sonAmigos = match.stream().anyMatch(s -> 
            (s.getEmisor().getId().equals(receptor.getId()) || s.getReceptor().getId().equals(receptor.getId()))
        );

        if (!sonAmigos) {
            throw new SecurityException("No puedes enviar mensajes a un usuario con el que no tienes un Match.");
        }

        // Creamos y guardamos el mensaje
        Mensaje mensaje = new Mensaje();
        mensaje.setEmisor(emisor);
        mensaje.setReceptor(receptor);
        mensaje.setContenido(contenido);

        Mensaje guardado = mensajeRepository.save(mensaje);
        MensajeDTO dto = mapearADTO(guardado);

        // El evento lleva el mensaje entero, no un aviso de "tienes algo nuevo".
        // Así el receptor lo pinta directamente y no hay que volver a pedir el
        // historial, que además podría llegar antes de que la fila sea visible.
        canalEventos.publicar(receptor.getId(), TipoEvento.MENSAJE, dto);

        return dto;
    }

    @Override
    @Transactional
    public List<MensajeDTO> obtenerHistorial(String emailUsuario, Long otroUsuarioId) {
        Usuario usuarioLogueado = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<Mensaje> historial = mensajeRepository.obtenerHistorialChat(usuarioLogueado.getId(), otroUsuarioId);

        // Abrir la conversacion es leerla. Se hace aqui y no en un endpoint
        // aparte para que no dependa de que el cliente se acuerde de avisar.
        mensajeRepository.marcarConversacionLeida(usuarioLogueado.getId(), otroUsuarioId);

        return historial.stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    @Override
    public List<ConversacionDTO> listarConversaciones(String emailUsuario) {
        Usuario yo = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Map<Long, Mensaje> ultimoPorCompanero = new HashMap<>();
        for (Mensaje m : mensajeRepository.ultimoMensajePorConversacion(yo.getId())) {
            ultimoPorCompanero.put(idDelOtro(m, yo.getId()), m);
        }

        Map<Long, Long> sinLeerPorCompanero = mensajeRepository.contarSinLeerPorEmisor(yo.getId()).stream()
                .collect(Collectors.toMap(SinLeerPorEmisor::emisorId, SinLeerPorEmisor::cuantos));

        // Se parte de los compañeros y no de los mensajes: alguien con quien
        // acabas de conectar y no has hablado tiene que salir igual, que es
        // justo cuando mas falta hace verlo.
        List<ConversacionDTO> conversaciones = new ArrayList<>();
        for (Usuario companero : companerosDe(yo.getId())) {
            Mensaje ultimo = ultimoPorCompanero.get(companero.getId());

            conversaciones.add(new ConversacionDTO(
                    companero.getId(),
                    companero.getNombre(),
                    companero.getAvatar(),
                    ultimo == null ? null : ultimo.getContenido(),
                    ultimo == null ? null : ultimo.getFechaEnvio(),
                    ultimo != null && ultimo.getEmisor().getId().equals(yo.getId()),
                    sinLeerPorCompanero.getOrDefault(companero.getId(), 0L)));
        }

        // Lo vivo arriba; los que aun no han dicho nada, al final por nombre en
        // vez de en el orden arbitrario en que los devuelva la base.
        conversaciones.sort(
                Comparator.comparing(ConversacionDTO::ultimaFecha,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ConversacionDTO::nombre, String.CASE_INSENSITIVE_ORDER));

        return conversaciones;
    }

    @Override
    @Transactional
    public void marcarConversacionLeida(String emailUsuario, Long otroUsuarioId) {
        usuarioRepository.findByEmail(emailUsuario).ifPresent(yo -> {
            mensajeRepository.marcarConversacionLeida(yo.getId(), otroUsuarioId);

            // Al que escribio, no al que lee: quien lee ya lo sabe. Sin esto el
            // "visto" solo aparece al recargar, o sea que miente justo cuando
            // los dos estais conectados, que es cuando alguien mira si le han
            // leido.
            canalEventos.publicar(otroUsuarioId, TipoEvento.MENSAJES_LEIDOS,
                    Map.of("conId", yo.getId()));
        });
    }

    @Override
    public long contarSinLeer(String emailUsuario) {
        return usuarioRepository.findByEmail(emailUsuario)
                .map(u -> mensajeRepository.countByReceptorIdAndLeidoFalse(u.getId()))
                .orElse(0L);
    }

    /** Los usuarios con los que hay una solicitud aceptada, sin repetir. */
    private List<Usuario> companerosDe(Long miId) {
        Map<Long, Usuario> porId = new LinkedHashMap<>();
        for (Solicitud s : solicitudRepository.findAceptadasPorUsuario(miId)) {
            Usuario otro = s.getEmisor().getId().equals(miId) ? s.getReceptor() : s.getEmisor();
            porId.putIfAbsent(otro.getId(), otro);
        }
        return new ArrayList<>(porId.values());
    }

    private Long idDelOtro(Mensaje m, Long miId) {
        return m.getEmisor().getId().equals(miId) ? m.getReceptor().getId() : m.getEmisor().getId();
    }

    // Método privado para limpiar los datos antes de enviarlos a Angular/Thunder Client
    private MensajeDTO mapearADTO(Mensaje m) {
        return new MensajeDTO(
                m.getId(),
                m.getEmisor().getId(),
                m.getEmisor().getNombre(),
                m.getReceptor().getId(),
                m.getContenido(),
                m.getFechaEnvio(),
                m.isLeido()
        );
    }
}