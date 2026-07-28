package com.spotterai.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La política de orígenes vive en un solo sitio.
 *
 * <p>Cinco controladores llevaban {@code @CrossOrigin} con "http://localhost:4200"
 * escrito a fuego, herencia del TFG que sobrevivió a hacer el origen
 * configurable. Nadie se dio cuenta durante meses porque en desarrollo el valor
 * a fuego y el configurado coincidían: solo se habría notado al desplegarlo en
 * otro sitio, que es el peor momento para enterarse.
 *
 * <p>Por eso esto mira el código fuente y no el comportamiento. Lo que hay que
 * impedir no es que el CORS falle —que no falla en local— sino que alguien
 * vuelva a escribir un origen donde no toca.
 */
class PoliticaDeOrigenesTest {

    private static final Path CONTROLADORES =
            Path.of("src", "main", "java", "com", "spotterai", "backend", "controllers");

    @Test
    @DisplayName("Ningún controlador declara su propia política de orígenes")
    void nadieSeSaltaLaConfiguracion() throws IOException {
        List<String> culpables = ficheros()
                .filter(PoliticaDeOrigenesTest::declaraCrossOrigin)
                .map(ruta -> ruta.getFileName().toString())
                .toList();

        assertTrue(culpables.isEmpty(),
                "El origen se configura con FRONTEND_ORIGIN en SecurityConfig, no en cada controlador. "
                        + "Lo declaran por su cuenta: " + culpables);
    }

    @Test
    @DisplayName("Ningún controlador lleva una dirección escrita a fuego")
    void ningunaDireccionAFuego() throws IOException {
        List<String> culpables = ficheros()
                .filter(PoliticaDeOrigenesTest::tieneDireccionLiteral)
                .map(ruta -> ruta.getFileName().toString())
                .toList();

        assertTrue(culpables.isEmpty(),
                "Hay una dirección de servidor en el código en vez de en la configuración: " + culpables);
    }

    private static Stream<Path> ficheros() throws IOException {
        return Files.list(CONTROLADORES).filter(ruta -> ruta.toString().endsWith(".java"));
    }

    private static boolean declaraCrossOrigin(Path ruta) {
        return contenido(ruta).lines().anyMatch(linea -> linea.stripLeading().startsWith("@CrossOrigin"));
    }

    private static boolean tieneDireccionLiteral(Path ruta) {
        // Solo fuera de comentarios: los javadoc de los endpoints usan la URL de
        // ejemplo a proposito, y prohibirla ahi seria empeorar la documentacion
        // para satisfacer a una prueba.
        return contenido(ruta).lines()
                .map(String::stripLeading)
                .filter(linea -> !linea.startsWith("*") && !linea.startsWith("//") && !linea.startsWith("/*"))
                .anyMatch(linea -> linea.contains("http://localhost") || linea.contains("https://localhost"));
    }

    private static String contenido(Path ruta) {
        try {
            return Files.readString(ruta);
        } catch (IOException e) {
            throw new IllegalStateException("No se ha podido leer " + ruta, e);
        }
    }
}
