package com.spotterai.backend.seguridad;

import com.spotterai.backend.config.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Los atributos de la galleta, que son la seguridad entera.
 *
 * <p>Una galleta mal puesta parece funcionar: se inicia sesión, se navega, todo
 * va. Lo que falla es justo lo que no se ve — que el JavaScript pueda leerla,
 * que viaje por http, que el cerrar sesión no la borre de verdad—. Por eso esto
 * se comprueba atributo a atributo y no "que el login devuelva 200".
 */
class GalletaDeSesionTest {

    private final JwtUtil jwt = Mockito.mock(JwtUtil.class);

    private GalletaDeSesion galleta(boolean segura) {
        Mockito.when(jwt.duracion()).thenReturn(Duration.ofHours(24));
        return new GalletaDeSesion(jwt, segura);
    }

    @Test
    @DisplayName("HttpOnly siempre: es la razón de ser de todo esto")
    void siempreHttpOnly() {
        // Sin este atributo el token vuelve a estar al alcance de cualquier
        // script de la página, que es exactamente de donde se le ha sacado.
        assertTrue(galleta(false).abrir("un-token").contains("HttpOnly"));
        assertTrue(galleta(true).abrir("un-token").contains("HttpOnly"));
    }

    @Test
    @DisplayName("SameSite=Lax: ni deja pasar el CSRF ni rompe los enlaces del correo")
    void sameSiteLax() {
        String cabecera = galleta(true).abrir("un-token");

        assertTrue(cabecera.contains("SameSite=Lax"), cabecera);
        // Con Strict, quien llega desde un enlace de nuestros propios avisos
        // aterriza sin sesión y ve el login aunque la tenga abierta.
        assertFalse(cabecera.contains("SameSite=Strict"), cabecera);
    }

    @Test
    @DisplayName("Secure se puede apagar, porque en localhost no hay https")
    void secureConfigurable() {
        // Una galleta Secure no viaja por http, así que con esto fijo a true
        // nadie podría iniciar sesión en desarrollo. En producción va en true.
        assertFalse(galleta(false).abrir("t").contains("Secure"));
        assertTrue(galleta(true).abrir("t").contains("Secure"));
    }

    @Test
    @DisplayName("La galleta caduca cuando caduca el token que lleva dentro")
    void caducaConElToken() {
        assertTrue(galleta(false).abrir("t").contains("Max-Age=" + Duration.ofHours(24).toSeconds()));
    }

    @Test
    @DisplayName("La de cerrar lleva los mismos atributos, o no borra nada")
    void cerrarBorraDeVerdad() {
        GalletaDeSesion g = galleta(true);
        String cerrar = g.cerrar();

        // Es el fallo clásico del cerrar sesión: si cambia un solo atributo, el
        // navegador la trata como otra galleta distinta y deja la de verdad
        // donde estaba. Parece que funciona porque la interfaz se limpia.
        assertTrue(cerrar.contains("Max-Age=0"), cerrar);
        assertTrue(cerrar.contains("HttpOnly"), cerrar);
        assertTrue(cerrar.contains("Secure"), cerrar);
        assertTrue(cerrar.contains("SameSite=Lax"), cerrar);
        assertTrue(cerrar.contains("Path=/"), cerrar);
    }

    @Test
    @DisplayName("Se lee el token de la petición, ignorando las demás galletas")
    void leeLaSuya() {
        HttpServletRequest peticion = Mockito.mock(HttpServletRequest.class);
        Mockito.when(peticion.getCookies()).thenReturn(new Cookie[]{
                new Cookie("otra", "nada"),
                new Cookie(GalletaDeSesion.NOMBRE, "el-token"),
                new Cookie("XSRF-TOKEN", "csrf"),
        });

        assertEquals(Optional.of("el-token"), GalletaDeSesion.leerDe(peticion));
    }

    @Test
    @DisplayName("Sin galletas, o con la nuestra vacía, no hay token")
    void sinGalletaNoHayToken() {
        HttpServletRequest ninguna = Mockito.mock(HttpServletRequest.class);
        Mockito.when(ninguna.getCookies()).thenReturn(null);
        assertTrue(GalletaDeSesion.leerDe(ninguna).isEmpty());

        // La de cerrar sesión deja la galleta con valor vacío. Si eso se tomara
        // por un token, el filtro intentaría validar "" en cada petición.
        HttpServletRequest vacia = Mockito.mock(HttpServletRequest.class);
        Mockito.when(vacia.getCookies())
                .thenReturn(new Cookie[]{new Cookie(GalletaDeSesion.NOMBRE, "")});
        assertTrue(GalletaDeSesion.leerDe(vacia).isEmpty());
    }
}
