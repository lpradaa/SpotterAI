package com.spotterai.backend.demo;

import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.semantica.IntencionesDeBiografia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Los tres ejes de las biografias de demostracion, ya leidos.
 *
 * <p>Hermano de {@link VectoresSembrados} y por el mismo motivo: que un
 * despliegue pueda correr <b>sin levantar el servicio del modelo</b>. Sin esto,
 * el noveno factor quedaria "sin datos" para toda la gente de demostracion, que
 * es justo la parte que la demo existe para enseñar.
 *
 * <p>Y ahora importa mas que antes. Cuando los vectores se sembraron, el
 * servicio ocupaba 475 MB; desde que lee intenciones son 611, asi que la opcion
 * de montar la demo sin gastar esa memoria vale mas, no menos.
 *
 * <h2>El formato, y por que no es base64</h2>
 *
 * <p>{@code demo/intenciones-biografia.tsv}: correo y tres numeros separados por
 * tabuladores, con el <b>hueco vacio</b> cuando esa biografia no habla de ese
 * eje. Los vectores iban en base64 porque eran 384 floats; esto son tres
 * numeros, y escribirlos en claro deja el fichero legible — se puede abrir y
 * entender por que una pareja de la demo puntua como puntua, que con el base64
 * era imposible.
 *
 * <p>El vacio no es un cero. Es "esta persona no ha dicho nada de esto", y es la
 * mitad de las celdas del fichero.
 */
final class IntencionesSembradas {

    private static final Logger log = LoggerFactory.getLogger(IntencionesSembradas.class);

    private static final String FICHERO = "demo/intenciones-biografia.tsv";

    /** Los tres ejes de alguien, con null donde no dijo nada. */
    private record Ejes(Double exigencia, Double ambicion, Double flexibilidad) {}

    private final Map<String, Ejes> porCorreo = new HashMap<>();

    IntencionesSembradas() {
        try (var entrada = new ClassPathResource(FICHERO).getInputStream();
             var lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {

            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank()) continue;

                // -1 en el limite: sin eso, split() se come los campos vacios
                // del final y una linea que termina en dos ejes sin valor
                // llegaria con menos columnas de las que tiene.
                String[] partes = linea.split("\t", -1);
                if (partes.length < 4) continue;

                porCorreo.put(partes[0].trim(),
                        new Ejes(numero(partes[1]), numero(partes[2]), numero(partes[3])));
            }

        } catch (Exception e) {
            // Que falte el fichero no puede impedir sembrar: la demo seguiria
            // siendo util, solo que sin el noveno factor.
            log.warn("No se han podido leer las intenciones sembradas ({}): "
                    + "la demo irá sin afinidad", e.getMessage());
        }
    }

    private static Double numero(String celda) {
        String limpia = celda == null ? "" : celda.trim();
        return limpia.isEmpty() ? null : Double.valueOf(limpia);
    }

    /**
     * Le pone a esta persona sus ejes, si los tenemos.
     *
     * <p>La huella se calcula aqui a partir de la biografia real y no viene en
     * el fichero. Asi, si alguien edita un texto de demostracion sin regenerar
     * esto, la huella deja de cuadrar, el repaso lo detecta como desfasado y los
     * ejes viejos no se quedan describiendo un texto que ya no existe.
     */
    void aplicarA(Usuario usuario) {
        Ejes ejes = porCorreo.get(usuario.getEmail());
        if (ejes == null || usuario.getBiografia() == null) return;

        usuario.setIntencionExigencia(ejes.exigencia());
        usuario.setIntencionAmbicion(ejes.ambicion());
        usuario.setIntencionFlexibilidad(ejes.flexibilidad());
        usuario.setIntencionesDe(IntencionesDeBiografia.huellaDe(usuario.getBiografia()));
    }

    int cuantos() {
        return porCorreo.size();
    }
}
