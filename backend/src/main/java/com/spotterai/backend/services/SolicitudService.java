package com.spotterai.backend.services;

import com.spotterai.backend.dtos.SolicitudDTO;
import com.spotterai.backend.models.Solicitud;
import java.util.List;

public interface SolicitudService {
    Solicitud crearSolicitud(Solicitud solicitud);
    Solicitud cambiarEstado(Long id, String nuevoEstado);
    SolicitudDTO enviarSolicitud(String emailEmisor, Long receptorId);
    SolicitudDTO responderSolicitud(String emailReceptor, Long solicitudId, String nuevoEstado);
    List<SolicitudDTO> obtenerPendientes(String emailReceptor);
    List<SolicitudDTO> obtenerAceptadas(String email);
}