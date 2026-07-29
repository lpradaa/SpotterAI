package com.spotterai.backend.avisos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cual de los dos carteros queda montado, segun la configuracion.
 *
 * <p>Esto solo se puede comprobar levantando el contexto, y por eso merece la
 * pena aunque cueste unos segundos: el fallo que evita no aparece en ninguna
 * prueba unitaria y no aparece hasta que se despliega. Con las dos condiciones
 * mal escritas hay dos carteros —y el contexto ni arranca— o ninguno, y
 * entonces nadie recibe nada y no hay ningun error que lo diga.
 *
 * <p>Sin configurar nada: el que escribe en el registro. Es el caso de cualquier
 * clon recien bajado y de todo el desarrollo.
 */
@SpringBootTest
class QueCarteroSeUsaTest {

    @Autowired
    private Map<String, Cartero> carteros;

    @Test
    @DisplayName("Sin configurar correo hay exactamente un cartero, y es el del registro")
    void sinConfigurarEsElDelRegistro() {
        assertEquals(1, carteros.size(), "Tiene que haber uno y solo uno: " + carteros.keySet());
        assertInstanceOf(CarteroAlRegistro.class, carteros.values().iterator().next());
    }
}

/**
 * Con {@code spotterai.correo.activo=true}: el que manda de verdad.
 *
 * <p>Comprueba ademas que el contexto levanta, o sea que {@code JavaMailSender}
 * existe y {@link CarteroSmtp} puede construirse. Sin el starter de correo en el
 * classpath eso reventaria aqui y no en produccion.
 *
 * <p>El {@code spring.mail.host} de aqui no es adorno: Spring solo monta el
 * {@code JavaMailSender} si esa propiedad esta puesta, asi que activar el correo
 * sin servidor configurado impide arrancar la aplicacion entera. En
 * {@code application.properties} tiene un valor por defecto justo para que eso
 * no pueda pasar; el fichero de los tests sustituye a ese, no lo completa, y por
 * eso hay que repetirlo.
 */
@SpringBootTest(properties = {
        "spotterai.correo.activo=true",
        "spring.mail.host=localhost",
})
class ConCorreoActivoTest {

    @Autowired
    private Map<String, Cartero> carteros;

    @Test
    @DisplayName("Con el correo activo hay exactamente un cartero, y es el de SMTP")
    void activoEsElDeSmtp() {
        assertEquals(1, carteros.size(), "Tiene que haber uno y solo uno: " + carteros.keySet());
        assertInstanceOf(CarteroSmtp.class, carteros.values().iterator().next());
    }
}
