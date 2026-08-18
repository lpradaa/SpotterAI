package com.spotterai.backend.migraciones;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Que las migraciones se puedan resolver antes de arrancar nada.
 *
 * <h2>Por que existe</h2>
 *
 * <p>Una migracion nueva salio numerada como V10 cuando ya habia una
 * {@code V10__baja_de_avisos.sql}. Flyway no dice nada al compilar ni en los
 * tests —corren con {@code spring.flyway.enabled=false} y H2— sino al arrancar
 * la aplicacion, y no arrancandola:
 *
 * <pre>Found more than one migration with version 10</pre>
 *
 * <p>Paso por delante de 389 pruebas en verde, de la build del frontend y de la
 * construccion de las dos imagenes Docker. Con {@code ddl-auto=validate} no hay
 * red debajo: la aplicacion no levanta.
 *
 * <p>El error de origen fue leer mal un {@code ls}: ordena alfabeticamente, asi
 * que V10…V17 salen <b>antes</b> que V1…V9 y la ultima de la lista parece la V9.
 * Esta prueba es la que convierte ese despiste en un fallo de cinco milisegundos
 * en vez de uno de despliegue.
 *
 * <h2>Por que lee del classpath</h2>
 *
 * <p>Y no del directorio de fuentes: el classpath es lo que ve Flyway. Un
 * fichero renombrado en {@code src} deja el viejo en {@code target/classes}
 * —copiar recursos no borra lo que sobra— y Flyway sigue viendo los dos. Eso
 * tambien costo una vuelta, asi que tambien se comprueba aqui.
 */
class MigracionesTest {

    /** V<numero>__<descripcion>.sql, que es lo que Flyway espera. */
    private static final Pattern NOMBRE = Pattern.compile("^V([0-9]+)__[a-z0-9_]+[.]sql$");

    private static List<File> migraciones() {
        URL carpeta = MigracionesTest.class.getResource("/db/migration");
        assertNotNull(carpeta, "No hay /db/migration en el classpath: ¿se han movido las migraciones?");

        // Por URI y no por getPath(): en Windows la ruta llega con el espacio de
        // "Proyecto Gym" escapado como %20 y File no lo entiende.
        File[] ficheros;
        try {
            ficheros = new File(carpeta.toURI()).listFiles();
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException("Ruta de migraciones ilegible: " + carpeta, e);
        }
        assertNotNull(ficheros, "No se ha podido leer /db/migration");

        return Arrays.stream(ficheros).filter(File::isFile).toList();
    }

    private static int versionDe(File fichero) {
        Matcher m = NOMBRE.matcher(fichero.getName());
        assertTrue(m.matches(),
                "«" + fichero.getName() + "» no sigue el patrón V<numero>__<descripcion>.sql, "
                        + "así que Flyway no la va a reconocer");
        return Integer.parseInt(m.group(1));
    }

    /**
     * El fallo que motiva todo esto.
     *
     * <p>Dos ficheros con el mismo numero y Flyway se niega a arrancar, porque
     * no puede saber cual va antes. El mensaje dice cuales son, que es lo que
     * uno quiere leer a las once de la noche.
     */
    @Test
    @DisplayName("No hay dos migraciones con la misma versión")
    void sinVersionesRepetidas() {
        Map<Integer, List<String>> porVersion = new HashMap<>();

        for (File fichero : migraciones()) {
            porVersion.computeIfAbsent(versionDe(fichero), v -> new ArrayList<>())
                    .add(fichero.getName());
        }

        List<String> repetidas = porVersion.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(e -> "V" + e.getKey() + " → " + e.getValue())
                .toList();

        assertTrue(repetidas.isEmpty(),
                "Flyway no arranca con versiones repetidas: " + repetidas
                        + ". Si acabas de renombrar una, mira si la vieja sigue en target/classes: "
                        + "copiar recursos no borra lo que sobra.");
    }

    /**
     * Sin huecos en la numeracion.
     *
     * <p>Flyway funciona igual con huecos, asi que esto no es suyo: un hueco
     * casi siempre significa que una migracion se perdio al juntar dos ramas, y
     * eso se descubre cuando a alguien le falta una tabla.
     */
    @Test
    @DisplayName("La numeración es consecutiva desde la 1")
    void sinHuecos() {
        List<Integer> versiones = migraciones().stream()
                .map(MigracionesTest::versionDe)
                .sorted()
                .toList();

        for (int i = 0; i < versiones.size(); i++) {
            assertEquals(i + 1, versiones.get(i),
                    "Falta la migración V" + (i + 1) + ": las versiones van "
                            + versiones + ". Un hueco suele ser un fichero perdido al juntar ramas.");
        }
    }

    /**
     * La siguiente es la que dice el numero mas alto, no la ultima del listado.
     *
     * <p>Es el despiste concreto que produjo el fallo: {@code ls} ordena por
     * texto y V17 no es lo ultimo que enseña. Dejarlo probado sirve de aviso a
     * quien venga a añadir la siguiente.
     */
    @Test
    @DisplayName("La última migración es la del número más alto")
    void laUltimaEsLaMasAlta() {
        int masAlta = migraciones().stream().mapToInt(MigracionesTest::versionDe).max().orElseThrow();

        assertEquals(migraciones().size(), masAlta,
                "Hay " + migraciones().size() + " migraciones y la más alta es la V" + masAlta
                        + ". La siguiente que se añada tiene que ser la V" + (masAlta + 1)
                        + " — ojo, que `ls` ordena por texto y enseña V1x antes que V9.");
    }
}
