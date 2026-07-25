package com.spotterai.backend.controllers;

import com.spotterai.backend.config.JwtUtil;
import com.spotterai.backend.dtos.AuthResponseDTO;
import com.spotterai.backend.dtos.UsuarioLoginDTO;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.services.UsuarioService;
import com.spotterai.backend.repositories.UsuarioRepository; // 👈 Importamos el repositorio legítimo

import org.springframework.beans.factory.annotation.Autowired; // 👈 Para la inyección limpia
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 💡 Inyectamos el repositorio directamente para guardar de verdad en MySQL sin tocar el Service
    @Autowired
    private UsuarioRepository usuarioRepository;

    public AuthController(UsuarioService usuarioService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.usuarioService = usuarioService;
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

    @PostMapping("/update-password")
    public ResponseEntity<?> updatePassword(@RequestBody UsuarioLoginDTO loginDTO) {
        // 1. Buscamos al usuario por el email que pasamos en el JSON
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(loginDTO.getEmail());

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            
            // 2. Encriptamos la nueva contraseña de forma nativa, limpia y completa
            usuario.setPassword(passwordEncoder.encode(loginDTO.getPassword()));
            usuarioRepository.save(usuario); // 💾 Persistimos el cambio en MySQL de forma definitiva
            
            // 3. Generamos un token fresco para el usuario actualizado
            String nuevoToken = jwtUtil.generarToken(usuario.getEmail());
            
            AuthResponseDTO respuesta = new AuthResponseDTO(
                    nuevoToken,
                    usuario.getId(),
                    usuario.getEmail(),
                    usuario.getNombre()
            );
            
            return ResponseEntity.ok(respuesta);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado.");
    }
}