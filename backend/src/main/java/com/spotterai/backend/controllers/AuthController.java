package com.spotterai.backend.controllers;

import com.spotterai.backend.config.JwtUtil;
import com.spotterai.backend.dtos.AuthResponseDTO;
import com.spotterai.backend.dtos.UsuarioLoginDTO;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.UsuarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UsuarioLoginDTO loginDTO) {
        
        // 1. Buscamos en la base de datos si hay alguien con ese email
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(loginDTO.getEmail());

        // 2. Si el usuario existe, comprobamos su contraseña de forma real y segura
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            
            // 3. Comprobamos exclusivamente con el validador criptográfico de BCrypt
            if (passwordEncoder.matches(loginDTO.getPassword(), usuario.getPassword())) {
                
                // Generamos el Token JWT legítimo
                String token = jwtUtil.generarToken(usuario.getEmail());
                
                // Empaquetamos la respuesta para el Frontend
                AuthResponseDTO respuesta = new AuthResponseDTO(
                        token,
                        usuario.getId(),
                        usuario.getEmail(),
                        usuario.getNombre()
                );
                
                return ResponseEntity.ok(respuesta);
            }
        }

        // 4. Si las credenciales no coinciden, denegamos el acceso
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Credenciales inválidas.");
    }
}