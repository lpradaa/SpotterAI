package com.spotterai.backend.controllers;

import com.spotterai.backend.config.JwtUtil;
import com.spotterai.backend.dtos.AuthResponseDTO;
import com.spotterai.backend.dtos.UsuarioLoginDTO;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.UsuarioRepository;
import com.spotterai.backend.seguridad.ControlDeIntentos;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** Mismo texto para todo lo que no sea un acceso válido. */
    private static final String CREDENCIALES_MAL = "Error: Credenciales inválidas.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ControlDeIntentos control;

    public AuthController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil, ControlDeIntentos control) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.control = control;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UsuarioLoginDTO loginDTO, HttpServletRequest peticion) {

        String claveCorreo = ControlDeIntentos.claveDeCorreo(loginDTO.getEmail());
        String claveDireccion = ControlDeIntentos.claveDeDireccion(direccionDe(peticion));

        // Antes de tocar la base: si ya se ha agotado el cupo, ni se comprueba.
        // Comprobar igualmente y responder lo mismo dejaría el coste de BCrypt
        // en pie, que es justo lo que un ataque por fuerza bruta consume.
        if (control.bloqueada(claveCorreo) || control.bloqueada(claveDireccion)) {
            Duration espera = maximo(control.esperaDe(claveCorreo), control.esperaDe(claveDireccion));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(Math.max(1, espera.toSeconds())))
                    .body("Demasiados intentos. Vuelve a probar en "
                            + Math.max(1, espera.toMinutes() + 1) + " minutos.");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(loginDTO.getEmail());

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            // Comprobamos exclusivamente con el validador criptográfico de BCrypt
            if (passwordEncoder.matches(loginDTO.getPassword(), usuario.getPassword())) {
                control.acierto(claveCorreo);

                String token = jwtUtil.generarToken(usuario.getEmail());

                return ResponseEntity.ok(new AuthResponseDTO(
                        token, usuario.getId(), usuario.getEmail(), usuario.getNombre()));
            }
        }

        // Cuenta también cuando el correo no existe. Si solo contaran los fallos
        // de cuentas reales, ver cuál se bloquea diría quién está registrado y el
        // freno acabaría siendo un listador de usuarios.
        control.fallo(claveCorreo);
        control.fallo(claveDireccion);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CREDENCIALES_MAL);
    }

    /**
     * De dónde viene la petición.
     *
     * <p>Detrás de nginx, {@code getRemoteAddr} es siempre el contenedor del
     * proxy, así que sin mirar la cabecera el límite por dirección metería a
     * todo el mundo en el mismo saco. Se lee la primera de la lista, que es la
     * del cliente.
     *
     * <p>Es información que el cliente puede inventarse, y por eso el límite por
     * dirección es un extra y no la defensa. La que no se puede falsear es la
     * del correo: para entrar en una cuenta hay que nombrarla.
     */
    private static String direccionDe(HttpServletRequest peticion) {
        String reenviada = peticion.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return peticion.getRemoteAddr();
    }

    private static Duration maximo(Duration una, Duration otra) {
        return una.compareTo(otra) >= 0 ? una : otra;
    }
}
