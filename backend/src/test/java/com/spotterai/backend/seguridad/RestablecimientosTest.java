package com.spotterai.backend.seguridad;

import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Recuperar una contraseña olvidada.
 *
 * <p>Hasta ahora no se podía: quien la olvidaba se quedaba fuera para siempre.
 * Lo que se fija aquí es lo que separa un restablecimiento de un agujero — que
 * el enlace caduque, que sirva una sola vez, que no diga quién está registrado
 * y que eche a las sesiones que hubiera abiertas.
 */
class RestablecimientosTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 10, 12, 0);

    private UsuarioRepository usuarios;
    private Restablecimientos restablecimientos;
    private final PasswordEncoder cifrador = new BCryptPasswordEncoder();

    private final Usuario luis = new Usuario();

    private Restablecimientos conReloj(LocalDateTime momento) {
        Clock reloj = Clock.fixed(momento.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        return new Restablecimientos(usuarios, cifrador, reloj);
    }

    @BeforeEach
    void preparar() {
        usuarios = Mockito.mock(UsuarioRepository.class);
        restablecimientos = conReloj(AHORA);

        luis.setId(1L);
        luis.setEmail("luis@test.com");
        luis.setPassword(cifrador.encode("la-de-siempre-123"));

        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));
        when(usuarios.findByEmail("luis@test.com")).thenReturn(Optional.of(luis));
        when(usuarios.findByEmail("nadie@test.com")).thenReturn(Optional.empty());
        when(usuarios.findByTokenReset(any())).thenReturn(Optional.empty());
    }

    /** Deja el token abierto y devuelve el valor que iría en el correo. */
    private String abrir() {
        String token = restablecimientos.abrirPara("luis@test.com").orElseThrow();
        when(usuarios.findByTokenReset(Restablecimientos.huellaDe(token)))
                .thenReturn(Optional.of(luis));
        return token;
    }

    @Test
    @DisplayName("En la base se guarda la huella, nunca el token")
    void enLaBaseSoloVaLaHuella() {
        String token = restablecimientos.abrirPara("luis@test.com").orElseThrow();

        // Con el token en claro, quien lea la base entra en la cuenta. Esta es la
        // diferencia con la llave de baja, que sí se guarda tal cual: con
        // aquélla lo máximo es dejar a alguien sin correos.
        assertNotEquals(token, luis.getTokenReset());
        assertEquals(Restablecimientos.huellaDe(token), luis.getTokenReset());
    }

    @Test
    @DisplayName("De un correo que no existe no se abre nada")
    void deUnCorreoQueNoExisteNoHayToken() {
        // Quien llama devuelve la misma respuesta en los dos casos: si no, este
        // formulario sería un comprobador de quién está registrado.
        assertTrue(restablecimientos.abrirPara("nadie@test.com").isEmpty());
    }

    @Test
    @DisplayName("Con el token bueno se cambia la contraseña")
    void elTokenBuenoFunciona() {
        String token = abrir();

        assertTrue(restablecimientos.consumir(token, "una-nueva-larga"));
        assertTrue(cifrador.matches("una-nueva-larga", luis.getPassword()));
    }

    @Test
    @DisplayName("Solo se puede usar una vez")
    void deUnSoloUso() {
        String token = abrir();
        restablecimientos.consumir(token, "una-nueva-larga");

        // El enlace se queda en la bandeja de entrada: si sirviera dos veces,
        // basta con que alguien lo reenvíe sin darse cuenta.
        when(usuarios.findByTokenReset(any())).thenReturn(Optional.empty());
        assertFalse(restablecimientos.consumir(token, "otra-mas-larga-aun"));
    }

    @Test
    @DisplayName("Pasada la hora ya no vale")
    void caduca() {
        String token = abrir();

        Restablecimientos tarde = conReloj(AHORA.plus(Restablecimientos.VALIDEZ).plusMinutes(1));

        // Un enlace de recuperación que no caduca es una llave permanente en un
        // buzón de correo.
        assertFalse(tarde.consumir(token, "una-nueva-larga"));
        assertTrue(cifrador.matches("la-de-siempre-123", luis.getPassword()));
    }

    @Test
    @DisplayName("Cambiar la contraseña echa a las sesiones abiertas")
    void echaALasSesionesAbiertas() {
        String token = abrir();
        restablecimientos.consumir(token, "una-nueva-larga");

        // Sin esto, quien te hubiera robado la sesión sigue dentro 24 horas y el
        // restablecimiento solo tranquiliza.
        assertEquals(AHORA, luis.getSesionesValidasDesde());
    }

    @Test
    @DisplayName("La marca va al segundo, que es la precisión del token")
    void laMarcaVaAlSegundo() {
        restablecimientos = conReloj(AHORA.withNano(500_000_000));
        String token = abrir();
        restablecimientos.consumir(token, "una-nueva-larga");

        // Sin truncar, quien entra dentro del mismo segundo se encuentra su token
        // recién emitido ya revocado: un fallo intermitente irreproducible.
        assertEquals(0, luis.getSesionesValidasDesde().getNano());
    }

    @Test
    @DisplayName("Una contraseña corta se rechaza, y el token sigue vivo")
    void unaContrasenaCortaNoGastaElToken() {
        String token = abrir();

        assertThrows(IllegalArgumentException.class,
                () -> restablecimientos.consumir(token, "corta"));

        // Si se gastara, la persona se queda sin enlace y sin cuenta por haber
        // escrito algo demasiado corto.
        assertNotNull(luis.getTokenReset());
        assertTrue(cifrador.matches("la-de-siempre-123", luis.getPassword()));
    }

    @Test
    @DisplayName("Un token inventado no cambia nada de nadie")
    void unTokenInventadoNoHaceNada() {
        assertFalse(restablecimientos.consumir("me-lo-invento", "una-nueva-larga"));
        assertFalse(restablecimientos.consumir(null, "una-nueva-larga"));
        assertFalse(restablecimientos.consumir("  ", "una-nueva-larga"));
    }

    @Test
    @DisplayName("Cambiarla desde dentro exige la actual")
    void cambiarExigeLaActual() {
        assertFalse(restablecimientos.cambiar("luis@test.com", "la-que-no-es", "una-nueva-larga"));
        assertTrue(cifrador.matches("la-de-siempre-123", luis.getPassword()));

        // Una sesión abierta en un ordenador prestado no debería bastar para
        // quedarse con la cuenta.
        assertTrue(restablecimientos.cambiar("luis@test.com", "la-de-siempre-123", "una-nueva-larga"));
        assertEquals(AHORA, luis.getSesionesValidasDesde());
    }
}
