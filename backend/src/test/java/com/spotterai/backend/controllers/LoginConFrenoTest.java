package com.spotterai.backend.controllers;

import com.spotterai.backend.config.JwtUtil;
import com.spotterai.backend.dtos.UsuarioLoginDTO;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.UsuarioRepository;
import com.spotterai.backend.seguridad.ControlDeIntentos;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El login con freno.
 *
 * <p>Lo que se protege aqui no es que bloquee —eso ya lo cubre
 * {@code ControlDeIntentosTest}— sino las dos decisiones del controlador que
 * son faciles de romper sin darse cuenta: que un correo inexistente cuente
 * igual que uno real, y que estando bloqueado no se llegue a comprobar la
 * contraseña.
 */
class LoginConFrenoTest {

    private UsuarioRepository usuarios;
    private PasswordEncoder cifrador;
    private ControlDeIntentos control;
    private AuthController controlador;

    private final Usuario alguien = new Usuario();

    @BeforeEach
    void preparar() {
        usuarios = Mockito.mock(UsuarioRepository.class);
        cifrador = Mockito.mock(PasswordEncoder.class);
        JwtUtil jwt = Mockito.mock(JwtUtil.class);
        control = new ControlDeIntentos(Clock.systemDefaultZone());

        controlador = new AuthController(usuarios, cifrador, jwt, control);

        alguien.setId(1L);
        alguien.setEmail("alguien@test.com");
        alguien.setNombre("Alguien");
        alguien.setPassword("$2a$hash");

        Mockito.when(usuarios.findByEmail("alguien@test.com")).thenReturn(Optional.of(alguien));
        Mockito.when(usuarios.findByEmail("nadie@test.com")).thenReturn(Optional.empty());
        Mockito.when(jwt.generarToken(Mockito.anyString())).thenReturn("un-token");
    }

    private HttpServletRequest desde(String ip) {
        HttpServletRequest peticion = Mockito.mock(HttpServletRequest.class);
        Mockito.when(peticion.getRemoteAddr()).thenReturn(ip);
        return peticion;
    }

    private ResponseEntity<?> intentar(String correo, String clave, String ip) {
        UsuarioLoginDTO dto = new UsuarioLoginDTO();
        dto.setEmail(correo);
        dto.setPassword(clave);
        return controlador.login(dto, desde(ip));
    }

    @Test
    @DisplayName("Con la contraseña buena se entra")
    void accesoNormal() {
        Mockito.when(cifrador.matches("buena", "$2a$hash")).thenReturn(true);

        assertEquals(200, intentar("alguien@test.com", "buena", "10.0.0.1").getStatusCode().value());
    }

    @Test
    @DisplayName("Al agotar los intentos deja de responder 401 y pasa a 429")
    void seCierra() {
        Mockito.when(cifrador.matches(Mockito.any(), Mockito.any())).thenReturn(false);

        for (int i = 0; i < ControlDeIntentos.MAX_POR_CORREO; i++) {
            assertEquals(401, intentar("alguien@test.com", "mala", "10.0.0.1").getStatusCode().value());
        }

        ResponseEntity<?> bloqueado = intentar("alguien@test.com", "mala", "10.0.0.1");
        assertEquals(429, bloqueado.getStatusCode().value());
        assertNotNull(bloqueado.getHeaders().getFirst("Retry-After"),
                "Sin Retry-After, quien llama no sabe cuando volver y reintenta a ciegas");
    }

    @Test
    @DisplayName("Bloqueado ya no se comprueba la contraseña")
    void bloqueadoNiSeMira() {
        Mockito.when(cifrador.matches(Mockito.any(), Mockito.any())).thenReturn(false);

        for (int i = 0; i < ControlDeIntentos.MAX_POR_CORREO; i++) {
            intentar("alguien@test.com", "mala", "10.0.0.1");
        }
        Mockito.clearInvocations(cifrador);

        intentar("alguien@test.com", "mala", "10.0.0.1");

        // BCrypt es caro a proposito, y ese coste es justo lo que un ataque por
        // fuerza bruta quiere consumir. Bloquear y seguir comprobando seria
        // dejar el gasto en pie.
        Mockito.verify(cifrador, Mockito.never()).matches(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("Un correo que no existe también gasta intentos")
    void elFrenoNoDelataQuienEstaRegistrado() {
        Mockito.when(cifrador.matches(Mockito.any(), Mockito.any())).thenReturn(false);

        for (int i = 0; i < ControlDeIntentos.MAX_POR_CORREO; i++) {
            intentar("nadie@test.com", "loquesea", "10.0.0.1");
        }

        // Si solo contaran los fallos de cuentas reales, ver cual se bloquea y
        // cual no diria exactamente quien esta registrado.
        assertEquals(429, intentar("nadie@test.com", "loquesea", "10.0.0.1").getStatusCode().value());
    }

    @Test
    @DisplayName("Bloquear una cuenta no bloquea a la de al lado")
    void noSeLlevaPorDelanteALosDemas() {
        Mockito.when(cifrador.matches(Mockito.any(), Mockito.any())).thenReturn(false);

        for (int i = 0; i < ControlDeIntentos.MAX_POR_CORREO; i++) {
            intentar("nadie@test.com", "loquesea", "10.0.0.1");
        }

        Mockito.when(cifrador.matches("buena", "$2a$hash")).thenReturn(true);

        // Misma IP y con el cupo de correo de otro agotado: tiene que entrar.
        assertEquals(200, intentar("alguien@test.com", "buena", "10.0.0.1").getStatusCode().value());
    }

    @Test
    @DisplayName("Entrar bien limpia los fallos anteriores de esa cuenta")
    void elAciertoPerdona() {
        Mockito.when(cifrador.matches(Mockito.any(), Mockito.any())).thenReturn(false);
        for (int i = 0; i < ControlDeIntentos.MAX_POR_CORREO - 1; i++) {
            intentar("alguien@test.com", "mala", "10.0.0.1");
        }

        Mockito.when(cifrador.matches("buena", "$2a$hash")).thenReturn(true);
        intentar("alguien@test.com", "buena", "10.0.0.1");

        Mockito.when(cifrador.matches("mala", "$2a$hash")).thenReturn(false);
        assertEquals(401, intentar("alguien@test.com", "mala", "10.0.0.1").getStatusCode().value(),
                "Tras entrar bien, el contador empieza de cero");
    }

    @Test
    @DisplayName("Un fallo no cambia el mensaje segun exista o no la cuenta")
    void elMensajeNoDistingue() {
        Mockito.when(cifrador.matches(Mockito.any(), Mockito.any())).thenReturn(false);

        Object existente = intentar("alguien@test.com", "mala", "10.0.0.1").getBody();
        Object inventada = intentar("nadie@test.com", "mala", "10.0.0.2").getBody();

        assertEquals(existente, inventada);
    }
}
