package com.spotterai.backend.controllers;

import com.spotterai.backend.config.IdiomaConfig;
import com.spotterai.backend.seguridad.Contrasenas;
import com.spotterai.backend.textos.ErrorDeNegocio;
import com.spotterai.backend.textos.ErrorDePermiso;
import com.spotterai.backend.textos.Textos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Que un error se le enseñe a alguien en su idioma.
 *
 * <p>Es el ultimo tramo de la traduccion del backend, y el que mas se ve: estos
 * textos llegan al frontend por {@code {"error": "..."}} y se pintan tal cual en
 * el chat, en el formulario de proponer y en el perfil. Antes la frase viajaba
 * escrita dentro de la excepcion, asi que salia en español pasara lo que pasara.
 *
 * <p>Lo que hay que vigilar de verdad es el paso intermedio: una excepcion que
 * lleva la clave y un {@code catch} que devuelve {@code getMessage()} no enseña
 * español, enseña <b>la clave</b>. Por eso el controlador ya no captura nada y
 * esto es lo unico que responde.
 */
@SpringBootTest
class ManejadorDeErroresTest {

    @Autowired
    private Textos textos;

    @Autowired
    private ManejadorDeErrores manejador;

    @AfterEach
    void devolverElIdioma() {
        // El locale vive en el hilo, que las pruebas comparten.
        LocaleContextHolder.setLocale(IdiomaConfig.ESPANOL);
    }

    private String errorDe(ResponseEntity<Map<String, String>> respuesta) {
        return respuesta.getBody().get("error");
    }

    @Test
    @DisplayName("La frase sale en el idioma de la peticion, no en el del servidor")
    void redactaConElIdiomaDeLaPeticion() {
        ErrorDeNegocio enElPasado = ErrorDeNegocio.de("error.sesion.enElPasado");

        LocaleContextHolder.setLocale(IdiomaConfig.ESPANOL);
        assertThat(errorDe(manejador.deNegocio(enElPasado)))
                .isEqualTo("No se puede quedar en el pasado.");

        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(errorDe(manejador.deNegocio(enElPasado)))
                .isEqualTo("You cannot meet in the past.");
    }

    @Test
    @DisplayName("Y nunca se le enseña la clave a nadie")
    void nuncaSaleLaClave() {
        ErrorDeNegocio e = ErrorDeNegocio.de("error.sesion.yaApuntada");

        // getMessage() sí es la clave, y está bien: es lo que sale en los
        // registros. Lo que no puede pasar es que salga por la respuesta.
        assertThat(e.getMessage()).isEqualTo("error.sesion.yaApuntada");
        assertThat(errorDe(manejador.deNegocio(e))).doesNotContain("error.sesion");
    }

    @Test
    @DisplayName("Un error con un hueco dentro se rellena igual")
    void rellenaLosHuecos() {
        ErrorDeNegocio e = ErrorDeNegocio.de("error.sesion.horaNoVale", "de fin");

        assertThat(errorDe(manejador.deNegocio(e))).isEqualTo("La hora de fin no vale.");
    }

    /**
     * Mientras queden excepciones con la frase dentro, se devuelven como
     * siempre. Sin este caso, migrarlas de una en una habría cambiado el código
     * de estado de las que faltaran.
     */
    @Test
    @DisplayName("Lo que todavia no tiene clave sigue funcionando igual")
    void loQueNoTieneClaveSigueIgual() {
        ResponseEntity<Map<String, String>> respuesta =
                manejador.sinTraducir(new IllegalArgumentException("Algo que aún no se ha migrado"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorDe(respuesta)).isEqualTo("Algo que aún no se ha migrado");
    }

    /**
     * Las tres puertas por las que se pone una contraseña —registro,
     * restablecimiento y cambio desde el perfil— pasan por la misma validación,
     * así que su texto se ve en tres pantallas distintas. Con el tope dentro de
     * la frase: si mañana sube de 12 a 14, el mensaje no puede quedarse diciendo
     * 12.
     */
    @Test
    @DisplayName("La regla de la contraseña se dice con su número y en los dos idiomas")
    void laReglaDeLaContrasena() {
        ErrorDeNegocio corta = assertThrows(ErrorDeNegocio.class,
                () -> Contrasenas.exigirQueValga("corta"));

        LocaleContextHolder.setLocale(IdiomaConfig.ESPANOL);
        assertThat(errorDe(manejador.deNegocio(corta)))
                .isEqualTo("La contraseña necesita al menos " + Contrasenas.MINIMO + " caracteres.");

        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(errorDe(manejador.deNegocio(corta)))
                .isEqualTo("The password needs at least " + Contrasenas.MINIMO + " characters.");
    }

    @Test
    @DisplayName("Mirar lo que no es tuyo sigue siendo un 403")
    void loDeOtroSigueSiendo403() {
        ResponseEntity<Map<String, String>> respuesta =
                manejador.deSeguridad(new SecurityException("No es tuyo"));

        // 400 y 403 son cosas distintas y el frontend las trata distinto: eso no
        // podía cambiar al centralizar los errores.
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Y ahora ese 403 también se traduce.
     *
     * <p>Hicieron falta dos clases hermanas porque Java no deja heredar de dos
     * sitios: lo que las separa es el código de estado, no el texto.
     */
    @Test
    @DisplayName("Un 403 se redacta igual que un 400, y sigue siendo 403")
    void elPermisoTambienSeTraduce() {
        ErrorDePermiso ajena = ErrorDePermiso.de("error.permiso.sesionAjena");

        LocaleContextHolder.setLocale(IdiomaConfig.ESPANOL);
        ResponseEntity<Map<String, String>> enEspanol = manejador.dePermiso(ajena);
        assertThat(enEspanol.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(errorDe(enEspanol)).isEqualTo("Esa sesión no es tuya.");

        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(errorDe(manejador.dePermiso(ajena))).isEqualTo("That session is not yours.");
    }

    @Test
    @DisplayName("Tampoco por aqui se le enseña la clave a nadie")
    void elPermisoNoEnseñaLaClave() {
        ErrorDePermiso e = ErrorDePermiso.de("error.permiso.hitoAjeno");

        assertThat(e.getMessage()).isEqualTo("error.permiso.hitoAjeno");
        assertThat(errorDe(manejador.dePermiso(e))).doesNotContain("error.permiso");
    }
}
