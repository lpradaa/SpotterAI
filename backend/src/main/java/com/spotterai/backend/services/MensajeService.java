package com.spotterai.backend.services;

import com.spotterai.backend.dtos.ConversacionDTO;
import com.spotterai.backend.dtos.MensajeDTO;
import java.util.List;

public interface MensajeService {
    MensajeDTO enviarMensaje(String emailEmisor, Long receptorId, String contenido);

    /** Marca de paso como leidos los que te habia mandado el otro. */
    List<MensajeDTO> obtenerHistorial(String emailUsuario, Long otroUsuarioId);

    /** Compañeros con su ultimo mensaje y lo que queda sin leer, listos para la lista. */
    List<ConversacionDTO> listarConversaciones(String emailUsuario);

    /** Total sin leer, para el indicador de la cabecera. */
    long contarSinLeer(String emailUsuario);

    /** Para mensajes que llegan con la conversacion ya abierta. */
    void marcarConversacionLeida(String emailUsuario, Long otroUsuarioId);
}
