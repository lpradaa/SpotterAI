package com.spotterai.backend.services;

import com.spotterai.backend.textos.ErrorDeNegocio;
import com.spotterai.backend.textos.ErrorDePermiso;
import com.spotterai.backend.dtos.DisponibilidadDTO;
import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.DisponibilidadRepository;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DisponibilidadServiceImpl implements DisponibilidadService {

    private final DisponibilidadRepository disponibilidadRepository;
    private final UsuarioRepository usuarioRepository; // NUEVO: Para buscar al usuario

    public DisponibilidadServiceImpl(DisponibilidadRepository disponibilidadRepository, UsuarioRepository usuarioRepository) {
        this.disponibilidadRepository = disponibilidadRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public DisponibilidadDTO agregarHorario(String emailUsuario, DisponibilidadDTO dto) {
        // 1. Validar lógica de tiempos
        if (dto.getHoraInicio().compareTo(dto.getHoraFin()) >= 0) {
            throw ErrorDeNegocio.de("error.horario.alReves");
        }

        // 2. Buscar al usuario
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // 3. Crear la Entidad y vincularla
        Disponibilidad nueva = new Disponibilidad();
        nueva.setDiaSemana(dto.getDiaSemana());
        nueva.setHoraInicio(dto.getHoraInicio());
        nueva.setHoraFin(dto.getHoraFin());
        nueva.setUsuario(usuario); // ¡Aquí ocurre la magia relacional!

        Disponibilidad guardada = disponibilidadRepository.save(nueva);

        return new DisponibilidadDTO(guardada.getId(), guardada.getDiaSemana(), guardada.getHoraInicio(), guardada.getHoraFin());
    }

    @Override
    public List<DisponibilidadDTO> obtenerHorariosDeUsuario(String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<Disponibilidad> horarios = disponibilidadRepository.findByUsuarioId(usuario.getId());

        // Transformamos la lista de Entidades a una lista de DTOs seguros
        return horarios.stream()
                .map(h -> new DisponibilidadDTO(h.getId(), h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin()))
                .collect(Collectors.toList());
    }

    @Override
    public void eliminarHorario(String emailUsuario, Long idHorario) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Disponibilidad horario = disponibilidadRepository.findById(idHorario)
                .orElseThrow(() -> new IllegalArgumentException("Horario no encontrado"));

        // COMPROBACIÓN DE SEGURIDAD CRÍTICA: ¿El horario es suyo?
        if (!horario.getUsuario().getId().equals(usuario.getId())) {
            throw ErrorDePermiso.de("error.permiso.horarioAjeno");
        }

        disponibilidadRepository.delete(horario);
    }
}