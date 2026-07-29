package com.spotterai.backend.seguridad;

import com.spotterai.backend.config.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * Donde vive la sesion: una galleta que el JavaScript de la pagina no puede leer.
 *
 * <h2>Que problema resuelve</h2>
 *
 * <p>El token vivia en {@code localStorage}, que es un almacen al que llega
 * cualquier script que se ejecute en la pagina. Eso convierte <b>cualquier</b>
 * XSS —uno propio, uno de una dependencia, uno de un anuncio— en robo de sesion:
 * dos lineas leen el token y se lo mandan a otro sitio, y ahi ya da igual que la
 * victima cierre el navegador, porque el token sigue valiendo veinticuatro
 * horas en manos de quien lo tenga.
 *
 * <p>Con {@code HttpOnly} el script no puede leerla. El navegador la manda sola
 * en cada peticion, asi que el XSS puede hacer peticiones en nombre de quien
 * este delante mientras la pagina siga abierta —eso no lo arregla nada— pero no
 * puede llevarse la credencial.
 *
 * <h2>Lo que esto obliga a traer detras</h2>
 *
 * <p>Una galleta se manda sola, y eso es justo lo que hace posible el CSRF: otra
 * pagina puede provocar peticiones a esta con la sesion puesta. Por eso van
 * juntos {@code SameSite=Lax} y la proteccion CSRF de Spring; ninguno de los dos
 * es opcional aqui, y por eso este cambio no se podia hacer a medias.
 *
 * <h2>SameSite=Lax y no Strict</h2>
 *
 * <p>Con {@code Strict}, quien llegue desde un enlace de un correo nuestro
 * aterriza sin sesion y ve el login, aunque la tenga abierta. Justo lo que hacen
 * los avisos que se acaban de escribir. {@code Lax} manda la galleta en la
 * navegacion de primer nivel y no en las peticiones que provoca otro sitio, que
 * es exactamente el reparto que hace falta.
 */
@Component
public class GalletaDeSesion {

    /** El nombre no dice que lleva dentro: no hay motivo para anunciarlo. */
    public static final String NOMBRE = "sa_sesion";

    private final JwtUtil jwt;
    private final boolean segura;

    public GalletaDeSesion(JwtUtil jwt,
                           @Value("${spotterai.sesion.cookie-segura}") boolean segura) {
        this.jwt = jwt;
        this.segura = segura;
    }

    /**
     * La galleta con la sesion recien abierta.
     *
     * <p>{@code Secure} es configurable y no siempre true por un motivo
     * practico: en desarrollo se entra por {@code http://localhost} y una
     * galleta {@code Secure} no se manda por http, asi que nadie podria iniciar
     * sesion. En cualquier despliegue de verdad va en true — y con HTTPS
     * delante, que es lo que dice el README.
     */
    public String abrir(String token) {
        return ResponseCookie.from(NOMBRE, token)
                .httpOnly(true)
                .secure(segura)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwt.duracion())
                .build()
                .toString();
    }

    /**
     * La galleta que borra la anterior.
     *
     * <p>Tiene que llevar exactamente los mismos atributos —mismo path, mismo
     * Secure, mismo SameSite— o el navegador la trata como otra distinta y deja
     * la de verdad donde estaba. Es el fallo clasico del cerrar sesion: parece
     * que funciona porque la interfaz se limpia, y la sesion sigue viva.
     */
    public String cerrar() {
        return ResponseCookie.from(NOMBRE, "")
                .httpOnly(true)
                .secure(segura)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build()
                .toString();
    }

    /** El token que trae la peticion, si trae alguno. */
    public static Optional<String> leerDe(HttpServletRequest peticion) {
        Cookie[] galletas = peticion.getCookies();
        if (galletas == null) return Optional.empty();

        return Arrays.stream(galletas)
                .filter(g -> NOMBRE.equals(g.getName()))
                .map(Cookie::getValue)
                .filter(valor -> valor != null && !valor.isBlank())
                .findFirst();
    }

    public static final String CABECERA = HttpHeaders.SET_COOKIE;
}
