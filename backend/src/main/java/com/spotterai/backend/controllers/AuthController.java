package com.spotterai.backend.controllers;

import com.spotterai.backend.config.JwtUtil;
import com.spotterai.backend.dtos.AuthResponseDTO;
import com.spotterai.backend.seguridad.GalletaDeSesion;
import jakarta.servlet.http.HttpServletResponse;
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

    private final GalletaDeSesion galleta;

    public AuthController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil, ControlDeIntentos control, GalletaDeSesion galleta) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.control = control;
        this.galleta = galleta;
    }

    /**
     * Cerrar sesion de verdad.
     *
     * <p>Antes bastaba con que el frontend se olvidara del token, porque lo
     * tenia el. Ahora la sesion la guarda el navegador en una galleta que el
     * JavaScript no puede tocar, asi que cerrarla es algo que solo puede hacer
     * el servidor: hay que pedirle que la borre.
     *
     * <p>Responde igual haya sesion o no. Quien llama a esto quiere quedarse sin
     * sesion, y no la tiene: eso es exito, no error.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse respuesta) {
        respuesta.addHeader(GalletaDeSesion.CABECERA, galleta.cerrar());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UsuarioLoginDTO loginDTO, HttpServletRequest peticion,
                                   HttpServletResponse respuesta) {

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

                // La galleta va en la respuesta del servlet y NO como cabecera
                // del ResponseEntity, y esto no es estilo: al volcar las
                // cabeceras del ResponseEntity, Spring reemplaza el valor de
                // Set-Cookie en vez de anadirlo, asi que borraba la galleta del
                // CSRF que FiltroGalletaCsrf acababa de escribir.
                //
                // El sintoma no era "falta una galleta". Era que entrabas, el
                // frontend pedia un ticket por POST sin token de CSRF, se
                // llevaba un 403, el interceptor lo leia como sesion invalida y
                // te devolvia al login diciendo que habia caducado. Se entraba y
                // se salia solo, y costo un rato encontrarlo.
                respuesta.addHeader(GalletaDeSesion.CABECERA, galleta.abrir(token));

                // El token sale en la galleta y NO en el cuerpo. Devolverlo
                // tambien aqui dejaria la puerta abierta a que alguien lo
                // guardara en localStorage, que es exactamente lo que este
                // cambio quita de en medio.
                return ResponseEntity.ok()
                        .body(new AuthResponseDTO(
                                usuario.getId(), usuario.getEmail(), usuario.getNombre(),
                                System.currentTimeMillis() + jwtUtil.duracion().toMillis()));
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
