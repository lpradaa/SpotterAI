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

    /**
     * Deshace la relacion con otra persona, sea cual sea su estado.
     *
     * Cubre dos cosas que no se podian hacer: retirar una solicitud enviada por
     * error y dejar de ser compañero de alguien. Sin esto, y con la restriccion
     * unica en su sitio, un clic equivocado bloqueaba ese par para siempre.
     */
    void deshacerRelacion(String email, Long otroUsuarioId);
}