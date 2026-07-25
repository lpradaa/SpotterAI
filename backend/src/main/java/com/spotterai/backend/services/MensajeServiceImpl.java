package com.spotterai.backend.services;

import com.spotterai.backend.dtos.MensajeDTO;
import com.spotterai.backend.eventos.CanalEventos;
import com.spotterai.backend.eventos.TipoEvento;
import com.spotterai.backend.models.Mensaje;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.repositories.MensajeRepository;
import com.spotterai.backend.repositories.UsuarioRepository;
import com.spotterai.backend.repositories.SolicitudRepository;
import org.springframework.stereotype.Service;

import java.util.List;
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
    public List<MensajeDTO> obtenerHistorial(String emailUsuario, Long otroUsuarioId) {
        Usuario usuarioLogueado = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<Mensaje> historial = mensajeRepository.obtenerHistorialChat(usuarioLogueado.getId(), otroUsuarioId);
        
        return historial.stream().map(this::mapearADTO).collect(Collectors.toList());
    }

    // Método privado para limpiar los datos antes de enviarlos a Angular/Thunder Client
    private MensajeDTO mapearADTO(Mensaje m) {
        return new MensajeDTO(
                m.getId(),
                m.getEmisor().getId(),
                m.getEmisor().getNombre(),
                m.getReceptor().getId(),
                m.getContenido(),
                m.getFechaEnvio()
        );
    }
}