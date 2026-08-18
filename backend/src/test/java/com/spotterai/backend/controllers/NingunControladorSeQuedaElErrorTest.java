package com.spotterai.backend.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Que ningun controlador capture un error y devuelva {@code getMessage()}.
 *
 * <h2>Por que existe</h2>
 *
 * <p>Este fallo ha aparecido <b>dos veces</b>, y las dos igual de callado.
 *
 * <p>Desde que las excepciones llevan la clave del texto en vez de la frase,
 * {@code getMessage()} devuelve {@code error.sesion.enElPasado}. El controlador
 * que la captura y la devuelve tal cual no enseña español: enseña la clave, en
 * pantalla, al usuario. Es peor que no haber traducido nada.
 *
 * <p>La primera vez fue en el registro, al migrar {@code Contrasenas}, y se
 * encontro por casualidad antes de cerrar. La segunda fueron <b>seis</b>
 * controladores de golpe —bloqueos, reportes, medios, horarios, hitos y el
 * perfil— al migrar los servicios que faltaban. Las 397 pruebas seguian en
 * verde: ninguna afirma lo que se lee en pantalla cuando algo falla, y escribir
 * esa prueba para cada endpoint seria mucho trabajo para cubrir un descuido
 * mecanico.
 *
 * <h2>Que mira, y que no</h2>
 *
 * <p>Solo la pareja concreta que hace daño: capturar la excepcion que ya lleva
 * clave y devolver su mensaje. Un {@code catch} que devuelve un 400 vacio o que
 * registra el fallo no molesta a nadie, y este test lo deja pasar.
 *
 * <p>Es un test de texto sobre ficheros, no de comportamiento. Se paga con que
 * no entiende Java —un comentario con esa forma lo despertaria— y se cobra en
 * que cubre los diecisiete controladores de una vez, incluidos los que aun no
 * existen.
 */
class NingunControladorSeQuedaElErrorTest {

    private static final Path CONTROLADORES =
            Paths.get("src/main/java/com/spotterai/backend/controllers");

    /**
     * Un {@code catch} de lo que ya lleva clave, con todo su cuerpo detras.
     *
     * <p>{@code SecurityException} entra igual: desde que existe
     * {@code ErrorDePermiso}, capturarla tiene el mismo efecto.
     */
    private static final Pattern CATCH = Pattern.compile(
            "catch\\s*\\(\\s*(IllegalArgumentException|SecurityException|ErrorDeNegocio|ErrorDePermiso)"
                    + "\\s+(\\w+)\\s*\\)\\s*\\{([^}]*)}",
            Pattern.DOTALL);

    @Test
    @DisplayName("Nadie captura un error con clave para devolver su mensaje")
    void nadieDevuelveLaClave() throws IOException {
        List<String> culpables = new ArrayList<>();

        try (Stream<Path> ficheros = Files.list(CONTROLADORES)) {
            for (Path fichero : ficheros.filter(f -> f.toString().endsWith(".java")).toList()) {
                // El manejador es justo el que tiene que hacer esto.
                if (fichero.getFileName().toString().equals("ManejadorDeErrores.java")) continue;

                String codigo = Files.readString(fichero, StandardCharsets.UTF_8);
                Matcher m = CATCH.matcher(codigo);

                while (m.find()) {
                    String variable = m.group(2);
                    String cuerpo = m.group(3);

                    if (cuerpo.contains(variable + ".getMessage()")) {
                        culpables.add(fichero.getFileName() + ": catch (" + m.group(1)
                                + ") que devuelve " + variable + ".getMessage()");
                    }
                }
            }
        }

        assertTrue(culpables.isEmpty(),
                "Estos controladores enseñarian la clave del texto en pantalla en vez de la "
                        + "frase. Quitar el catch basta: ManejadorDeErrores la redacta con el "
                        + "idioma de la peticion.\n  " + String.join("\n  ", culpables));
    }

    /** Si esto falla, el que se ha movido es el test. */
    @Test
    @DisplayName("La carpeta de controladores esta donde este test cree")
    void miraDondeDebe() throws IOException {
        assertTrue(Files.isDirectory(CONTROLADORES), "No existe: " + CONTROLADORES.toAbsolutePath());

        try (Stream<Path> ficheros = Files.list(CONTROLADORES)) {
            long cuantos = ficheros.filter(f -> f.toString().endsWith("Controller.java")).count();
            assertTrue(cuantos >= 10, "Solo " + cuantos + " controladores: algo no cuadra");
        }
    }
}
