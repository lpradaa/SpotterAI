package com.spotterai.backend.textos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Que los dos catalogos digan lo mismo.
 *
 * <h2>Por que existe</h2>
 *
 * <p>En el frontend esto no hace falta: {@code en.ts} esta tipado contra
 * {@code es.ts}, asi que una clave sin traducir <b>no compila</b>. Aqui no hay
 * nada equivalente: {@code messages_en.properties} es un fichero de texto suelto
 * y a nadie le importa lo que le falte.
 *
 * <p>Y el fallo no se ve trabajando: cuando falta la clave inglesa, Java cae al
 * catalogo por defecto y la frase sale <b>en español</b>, bien formada, dentro
 * de una pantalla en ingles. Igual que los valores guardados sin etiquetar en el
 * frontend — el mismo error, y por eso la misma clase de red.
 *
 * <h2>Las tres cosas que mira</h2>
 *
 * <p>La tercera es la que no se le ocurre a nadie hasta que pasa: un
 * {@code .properties} con la clave repetida no avisa, se queda con la ultima y
 * la primera desaparece en silencio. Ya hubo un susto parecido al escribir los
 * correos con saltos de linea de verdad, que partieron las claves y dejaron
 * media seccion muerta sin una sola queja.
 */
class CatalogosTest {

    private static final String ES = "messages.properties";
    private static final String EN = "messages_en.properties";

    /** Los huecos de una frase: {0}, {1}… */
    private static final Pattern HUECO = Pattern.compile("\\{(\\d+)}");

    @Test
    @DisplayName("las dos lenguas tienen exactamente las mismas claves")
    void mismasClaves() {
        Set<String> es = leer(ES).keySet();
        Set<String> en = leer(EN).keySet();

        Set<String> sinTraducir = new TreeSet<>(es);
        sinTraducir.removeAll(en);
        assertTrue(sinTraducir.isEmpty(),
                "Estas claves solo estan en español, asi que en una pantalla en ingles "
                        + "saldra la frase española sin que nada falle: " + sinTraducir);

        Set<String> sobran = new TreeSet<>(en);
        sobran.removeAll(es);
        assertTrue(sobran.isEmpty(),
                "Estas claves solo estan en ingles: o falta la española o sobran ellas. "
                        + "Un catalogo que no se usa envejece sin que nadie lo note: " + sobran);
    }

    /**
     * Los huecos tienen que ser los mismos en las dos.
     *
     * <p>Olvidar un {@code {0}} al traducir no rompe nada visible: la frase sale
     * entera y bien escrita, solo que sin el dato. «El archivo pasa de MB» se lee
     * como una errata, no como una traduccion incompleta.
     */
    @Test
    @DisplayName("una frase no pierde sus huecos al traducirse")
    void mismosHuecos() {
        Map<String, String> es = leer(ES);
        Map<String, String> en = leer(EN);

        List<String> descuadres = new ArrayList<>();
        for (Map.Entry<String, String> entrada : es.entrySet()) {
            String ingles = en.get(entrada.getKey());
            if (ingles == null) continue;   // eso ya lo dice la otra prueba

            Set<String> aqui = huecos(entrada.getValue());
            Set<String> alli = huecos(ingles);
            if (!aqui.equals(alli)) {
                descuadres.add(entrada.getKey() + " español=" + aqui + " ingles=" + alli);
            }
        }

        assertTrue(descuadres.isEmpty(),
                "Estas frases pierden o ganan un dato al cambiar de idioma: " + descuadres);
    }

    @Test
    @DisplayName("ninguna clave esta repetida dentro de un fichero")
    void sinClavesRepetidas() {
        for (String fichero : List.of(ES, EN)) {
            List<String> repetidas = new ArrayList<>();
            Set<String> vistas = new LinkedHashSet<>();

            for (String linea : lineas(fichero)) {
                String limpia = linea.strip();
                if (limpia.isEmpty() || limpia.startsWith("#") || !limpia.contains("=")) continue;

                String clave = limpia.substring(0, limpia.indexOf('=')).strip();
                if (!vistas.add(clave)) repetidas.add(clave);
            }

            assertTrue(repetidas.isEmpty(),
                    fichero + " tiene claves repetidas. Un .properties se queda con la "
                            + "ultima y pierde la primera sin avisar: " + repetidas);
        }
    }

    // ---------------------------------------------------------------- ayudas

    private static Set<String> huecos(String frase) {
        Set<String> encontrados = new TreeSet<>();
        Matcher m = HUECO.matcher(frase);
        while (m.find()) encontrados.add(m.group(1));
        return encontrados;
    }

    /**
     * Lee del classpath, que es de donde lo lee Spring.
     *
     * <p>Y no con {@code Properties.load}, que no distingue una clave repetida
     * de una sola: se queda con la ultima y no lo cuenta.
     */
    private static Map<String, String> leer(String fichero) {
        Map<String, String> catalogo = new LinkedHashMap<>();
        for (String linea : lineas(fichero)) {
            String limpia = linea.strip();
            if (limpia.isEmpty() || limpia.startsWith("#") || !limpia.contains("=")) continue;

            int igual = limpia.indexOf('=');
            catalogo.put(limpia.substring(0, igual).strip(), limpia.substring(igual + 1));
        }
        return catalogo;
    }

    private static List<String> lineas(String fichero) {
        try (InputStream flujo = CatalogosTest.class.getClassLoader().getResourceAsStream(fichero)) {
            assertNotNull(flujo, "No esta en el classpath: " + fichero);

            List<String> todas = new ArrayList<>();
            try (BufferedReader lector =
                         new BufferedReader(new InputStreamReader(flujo, StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = lector.readLine()) != null) todas.add(linea);
            }
            assertEquals(true, !todas.isEmpty(), fichero + " esta vacio");
            return todas;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer " + fichero, e);
        }
    }
}
