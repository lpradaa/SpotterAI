package com.spotterai.backend.services;

import com.spotterai.backend.dtos.PerfilPublicoDTO;
import com.spotterai.backend.dtos.ActividadDTO;
import com.spotterai.backend.dtos.UsuarioPerfilDTO;
import com.spotterai.backend.dtos.UsuarioRegistroDTO;
import com.spotterai.backend.dtos.UsuarioResponseDTO;
import com.spotterai.backend.matching.ExplicacionMatch;
import com.spotterai.backend.models.Usuario;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UsuarioService {
    UsuarioResponseDTO registrarUsuario(UsuarioRegistroDTO dto);
    Optional<Usuario> buscarPorEmail(String email);
    List<Usuario> obtenerTodosLosUsuarios();
    UsuarioResponseDTO actualizarPerfil(String email, UsuarioPerfilDTO dto);

    /** Candidatos puntuados por compatibilidad, de mayor a menor. Sin IA: rapido y barato. */
    List<UsuarioResponseDTO> buscarCompañeros(String email);

    /** Explicacion redactada de un match concreto. Cuesta una llamada a la API, va bajo demanda. */
    ExplicacionMatch explicarMatch(String email, Long otroUsuarioId);

    Map<String, Object> obtenerMiPerfilCompleto(String email);

    /**
     * La ficha de otra persona: quien es, que ha conseguido y como encajais.
     *
     * No hacia falta hasta ahora porque no se podia mirar a nadie, solo pulsar
     * "Conectar" sobre una fila.
     */
    PerfilPublicoDTO verPerfilDe(String email, Long otroUsuarioId);

    /**
     * Lo que han hecho ultimamente las personas con las que ya entrenas.
     *
     * <p>Acotado a los companeros a proposito: es lo que hace que el tablero deje
     * de ser un panel de control y se note que hay gente al otro lado, sin abrir
     * un muro publico al que la gente publique para que la vean.
     */
    List<ActividadDTO> actividadDeCompaneros(String email);

    /**
     * Lo que puntua una pareja ahora mismo, sin explicacion ni ficha.
     *
     * <p>Existe para congelarlo en la solicitud, que es lo unico que permite
     * preguntar despues si el motor acierta. Va por el mismo cargador que la
     * lista, la ficha y la explicacion, y no por un camino propio: son cuatro
     * llamadas al mismo sitio, que es justo lo que la sobrecarga borrada hacia
     * imposible.
     */
    int compatibilidadEntre(Long unoId, Long otroId);
}
