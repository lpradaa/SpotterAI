package com.spotterai.backend.controllers;

import com.spotterai.backend.config.JwtUtil;
import com.spotterai.backend.dtos.AuthResponseDTO;
import com.spotterai.backend.seguridad.GalletaDeSesion;
import jakarta.servlet.http.HttpServletResponse;
import com.spotterai.backend.dtos.UsuarioLoginDTO;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.UsuarioRepository;
import com.spotterai.backend.avisos.Cartero;
import com.spotterai.backend.avisos.RedactorDeAvisos;
import com.spotterai.backend.seguridad.ControlDeIntentos;
import com.spotterai.backend.seguridad.Restablecimientos;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
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
    private final Restablecimientos restablecimientos;
    private final RedactorDeAvisos redactor;
    private final Cartero cartero;

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil, ControlDeIntentos control, GalletaDeSesion galleta,
                          Restablecimientos restablecimientos, RedactorDeAvisos redactor,
                          Cartero cartero) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.control = control;
        this.galleta = galleta;
        this.restablecimientos = restablecimientos;
        this.redactor = redactor;
        this.cartero = cartero;
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
    /**
     * "He olvidado mi contraseña".
     *
     * <p><b>Responde siempre lo mismo</b>, exista la cuenta o no. Si contestara
     * distinto, este formulario seria un comprobador de quien esta registrado:
     * abierto, sin sesion y con solo meter correos. Eso importa especialmente en
     * esta aplicacion, donde estar dado de alta dice algo de la persona.
     *
     * <p>Pasa por el mismo freno que el login y por correo, no por direccion: sin
     * eso, esto es un boton para inundar el buzon de cualquiera.
     */
    @PostMapping("/olvide")
    public ResponseEntity<?> olvide(@RequestBody Map<String, String> cuerpo) {
        String email = cuerpo.getOrDefault("email", "").trim();
        String clave = ControlDeIntentos.claveDeCorreo("olvide:" + email);

        if (!email.isBlank() && !control.bloqueada(clave)) {
            control.fallo(clave);

            restablecimientos.abrirPara(email).ifPresent(token ->
                    usuarioRepository.findByEmail(email).ifPresent(usuario -> {
                        try {
                            cartero.enviar(redactor.paraRestablecer(usuario, token));
                        } catch (Exception e) {
                            // Que el correo no salga no puede cambiar la respuesta:
                            // decir "no se ha podido enviar" tambien delata que la
                            // cuenta existe.
                            log.warn("No se ha podido mandar el correo de recuperacion: {}", e.getMessage());
                        }
                    }));
        }

        return ResponseEntity.accepted().body(Map.of(
                "mensaje", "Si ese correo tiene cuenta, te hemos mandado un enlace."));
    }

    /** Poner la contraseña nueva con el token del correo. */
    @PostMapping("/restablecer")
    public ResponseEntity<?> restablecer(@RequestBody Map<String, String> cuerpo) {
        try {
            boolean hecho = restablecimientos.consumir(
                    cuerpo.get("token"), cuerpo.get("password"));

            if (!hecho) {
                return ResponseEntity.status(HttpStatus.GONE).body(Map.of(
                        "error", "Ese enlace ya no vale. Pide uno nuevo."));
            }
            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            // La regla de la contraseña, tal cual, para poder enseñarla.
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cambiar la contraseña estando dentro.
     *
     * <p>Se pide la actual aunque haya sesion: una sesion abierta en un ordenador
     * prestado no deberia bastar para quedarse con la cuenta.
     *
     * <p>Al cambiarla se invalidan todas las sesiones, incluida esta. Es
     * deliberado y la pantalla lo dice: si cambias la contraseña porque crees
     * que alguien ha entrado, lo que quieres es justo eso.
     */
    @PostMapping("/contrasena")
    public ResponseEntity<?> cambiarContrasena(@RequestBody Map<String, String> cuerpo,
                                               HttpServletResponse respuesta) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            boolean hecho = restablecimientos.cambiar(
                    email, cuerpo.get("actual"), cuerpo.get("nueva"));

            if (!hecho) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", "La contraseña actual no es correcta."));
            }

            // La galleta de esta sesion tambien deja de valer, asi que se borra:
            // dejarla puesta significaria un 403 en cada peticion sin que la
            // interfaz supiera por que.
            respuesta.addHeader(GalletaDeSesion.CABECERA, galleta.cerrar());
            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

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
