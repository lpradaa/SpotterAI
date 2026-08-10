package com.spotterai.backend.controllers;

import com.spotterai.backend.config.JwtUtil;
import com.spotterai.backend.seguridad.GalletaDeSesion;
import com.spotterai.backend.dtos.UsuarioLoginDTO;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.UsuarioRepository;
import com.spotterai.backend.seguridad.ControlDeIntentos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
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
        Mockito.when(jwt.duracion()).thenReturn(java.time.Duration.ofHours(24));
        control = new ControlDeIntentos(Clock.systemDefaultZone());

        controlador = new AuthController(usuarios, cifrador, jwt, control,
                new GalletaDeSesion(jwt, false),
                Mockito.mock(com.spotterai.backend.seguridad.Restablecimientos.class),
                new com.spotterai.backend.avisos.RedactorDeAvisos("http://localhost:4200"),
                aviso -> { });

        alguien.setId(1L);
        alguien.setEmail("alguien@test.com");
        alguien.setNombre("Alguien");
        alguien.setPassword("$2a$hash");

        Mockito.when(usuarios.findByEmail("alguien@test.com")).thenReturn(Optional.of(alguien));
        Mockito.when(usuarios.findByEmail("nadie@test.com")).thenReturn(Optional.empty());
        Mockito.when(jwt.generarToken(Mockito.anyString())).thenReturn("un-token");
    }

    /**
     * Una peticion de verdad y no un doble.
     *
     * <p>Con un mock a secas, todo lo que no se programe devuelve null, y el
     * repositorio de CSRF necesita el contextPath para escribir la galleta:
     * revienta con un NullPointerException que no dice nada de lo que se esta
     * probando aqui.
     */
    private HttpServletRequest desde(String ip) {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.setRemoteAddr(ip);
        return peticion;
    }

    private UsuarioLoginDTO credenciales(String correo, String clave) {
        UsuarioLoginDTO dto = new UsuarioLoginDTO();
        dto.setEmail(correo);
        dto.setPassword(clave);
        return dto;
    }

    private ResponseEntity<?> intentar(String correo, String clave, String ip) {
        UsuarioLoginDTO dto = new UsuarioLoginDTO();
        dto.setEmail(correo);
        dto.setPassword(clave);
        return controlador.login(dto, desde(ip), new org.springframework.mock.web.MockHttpServletResponse());
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

    @Test
    @DisplayName("El login pone la sesión en la galleta y NO la devuelve en el cuerpo")
    void elTokenNoViajaEnElCuerpo() {
        Mockito.when(cifrador.matches(Mockito.any(), Mockito.any())).thenReturn(true);

        // La respuesta del servlet y no el ResponseEntity: las galletas se
        // escriben ahi para que la de sesion y la de CSRF no se pisen.
        MockHttpServletResponse salida = new MockHttpServletResponse();
        var respuesta = controlador.login(credenciales("alguien@test.com", "buena"),
                desde("10.0.0.9"), salida);

        assertEquals(200, respuesta.getStatusCode().value());

        // Todas, no la primera: el login escribe dos —sesión y CSRF— y cuál sale
        // antes es un detalle. Que salgan las dos es justo lo que estuvo roto:
        // poner una en el ResponseEntity hacía desaparecer la otra.
        var galletas = salida.getHeaders("Set-Cookie");

        String sesion = galletas.stream().map(String::valueOf)
                .filter(g -> g.startsWith("sa_sesion="))
                .findFirst().orElse(null);

        assertNotNull(sesion, "Sin galleta de sesión no hay sesión: " + galletas);
        assertTrue(sesion.contains("un-token"), sesion);
        assertTrue(sesion.contains("HttpOnly"), sesion);

        // La del CSRF la escribe FiltroGalletaCsrf, que aquí no corre: lo que
        // se comprueba en esta prueba es que la de sesión no la pisa, y eso se
        // ve en que sigue saliendo cuando hay otras. El caso completo —las dos
        // en la misma respuesta— se comprobó contra la aplicación arrancada.

        // Devolverlo también en el cuerpo dejaría la puerta abierta a que
        // alguien lo guardara en localStorage, que es de donde se ha sacado.
        assertFalse(String.valueOf(respuesta.getBody()).contains("un-token"),
                String.valueOf(respuesta.getBody()));
    }

    @Test
    @DisplayName("Cerrar sesión borra la galleta, aunque no hubiera ninguna")
    void cerrarSesionBorraLaGalleta() {
        MockHttpServletResponse salida = new MockHttpServletResponse();
        controlador.logout(salida);
        String galleta = salida.getHeader("Set-Cookie");

        assertNotNull(galleta);
        assertTrue(galleta.contains("Max-Age=0"), galleta);
    }
}
