package com.spotterai.backend.services;

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

    List<UsuarioResponseDTO> explorarComunidad(String email);
    Map<String, Object> obtenerMiPerfilCompleto(String email);
}