package com.spotterai.backend.services;

import com.spotterai.backend.dtos.NuevaSesionDTO;
import com.spotterai.backend.dtos.SesionDTO;
import com.spotterai.backend.dtos.SugerenciaSesionDTO;

import java.util.List;
import java.util.Optional;

/**
 * Quedar de verdad: proponer un dia y una hora, responder, y apuntar lo que
 * salio de ahi.
 */
public interface SesionService {

    /** Lo que el formulario trae ya puesto, sacado del solape real. */
    SugerenciaSesionDTO sugerir(String email, Long otroUsuarioId);

    /** Propone una sesion. Solo entre compañeros y solo hacia el futuro. */
    SesionDTO proponer(String email, Long otroUsuarioId, NuevaSesionDTO datos);

    /** Acepta o rechaza. Solo el invitado, y solo mientras siga pendiente. */
    SesionDTO responder(String email, Long sesionId, boolean acepta);

    /** Cualquiera de los dos, mientras no haya empezado. */
    void cancelar(String email, Long sesionId);

    /**
     * Deja constancia de que esta persona fue, y lo apunta en su historial.
     *
     * <p>Cada uno confirma lo suyo: que el otro diga que entrenasteis no prueba
     * que tu fueras.
     */
    SesionDTO confirmar(String email, Long sesionId);

    /** Lo que sigue contando: por responder, por delante y por apuntar. */
    List<SesionDTO> mias(String email);

    /**
     * Cuántas propuestas esperan tu respuesta, para el aviso de la cabecera.
     *
     * <p>Una sesión sin contestar es la misma clase de cosa que una solicitud o
     * un mensaje sin leer: algo que espera algo de ti. Faltaba en el contador, y
     * eso significaba que al salir del tablero dejabas de saber que la tenías.
     */
    long pendientesParaMi(String email);

    /** La sesion viva con otra persona, para poder enseñarla dentro del chat. */
    Optional<SesionDTO> conMigo(String email, Long otroUsuarioId);
}
