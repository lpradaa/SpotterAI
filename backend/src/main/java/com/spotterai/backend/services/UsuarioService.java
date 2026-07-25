package com.spotterai.backend.services;

import com.spotterai.backend.dtos.UsuarioPerfilDTO;
import com.spotterai.backend.dtos.UsuarioRegistroDTO;
import com.spotterai.backend.dtos.UsuarioResponseDTO;
import com.spotterai.backend.models.Usuario;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UsuarioService {
    UsuarioResponseDTO registrarUsuario(UsuarioRegistroDTO dto);
    Optional<Usuario> buscarPorEmail(String email);
    List<Usuario> obtenerTodosLosUsuarios();
    UsuarioResponseDTO actualizarPerfil(String email, UsuarioPerfilDTO dto);
    List<UsuarioResponseDTO> buscarCompañeros(String email);
    List<UsuarioResponseDTO> explorarComunidad(String email);
    Map<String, Object> obtenerMiPerfilCompleto(String email);
}