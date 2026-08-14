package com.spotterai.backend.demo;

import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.semantica.VectorDeBiografia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Los vectores de las biografias de demostracion, ya calculados.
 *
 * <p>Vienen en un fichero en vez de calcularse al arrancar para que un
 * despliegue pueda correr sin levantar el servicio de embeddings. Sin ellos, el
 * noveno factor del motor quedaria "sin datos" para toda la gente de
 * demostracion: justo la parte que la demo existe para enseñar.
 *
 * <p>Nacieron de una restriccion mas dura: el servicio ocupaba 756 MB y no cabia
 * en las capas gratuitas de 512. Hoy son 475 —se le quito PyTorch y el modelo va
 * cuantizado a int8— asi que ya cabe, y esto pasa de ser obligatorio a ser una
 * opcion: montar la demo sin gastar la memoria de un modelo.
 *
 * <p>Asi que la demo se despliega con sus vectores hechos. Es una decision de
 * despliegue, no un atajo del motor: en un despliegue sin el servicio, quien escriba una
 * biografia nueva se quedara sin vector hasta que alguien lo levante y corra el
 * repaso. Esta dicho en docs/despliegue.md.
 *
 * <p>El fichero es {@code demo/vectores-biografia.tsv}: correo, tabulador, y el
 * vector en base64. Se genero desde la base local con el modelo real corriendo.
 */
final class VectoresSembrados {

    private static final Logger log = LoggerFactory.getLogger(VectoresSembrados.class);

    private static final String FICHERO = "demo/vectores-biografia.tsv";

    private final Map<String, byte[]> porCorreo = new HashMap<>();

    VectoresSembrados() {
        try (var entrada = new ClassPathResource(FICHERO).getInputStream();
             var lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {

            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank()) continue;

                String[] partes = linea.split("\t", 2);
                if (partes.length != 2) continue;

                porCorreo.put(partes[0].trim(), Base64.getDecoder().decode(partes[1].trim()));
            }

        } catch (Exception e) {
            // Que falte el fichero no puede impedir sembrar: la demo seguiria
            // siendo util, solo que sin el factor semantico.
            log.warn("No se han podido leer los vectores sembrados ({}): la demo irá sin afinidad", e.getMessage());
        }
    }

    /**
     * Le pone a esta persona su vector, si lo tenemos.
     *
     * <p>La huella se calcula aqui a partir de la biografia real, no viene en el
     * fichero. Asi, si alguien edita un texto de demostracion sin regenerar los
     * vectores, la huella deja de cuadrar, el repaso lo detecta como desfasado y
     * el vector viejo no se queda describiendo un texto que ya no existe.
     */
    void aplicarA(Usuario usuario) {
        byte[] vector = porCorreo.get(usuario.getEmail());
        if (vector == null || usuario.getBiografia() == null) return;

        usuario.setBiografiaVector(vector);
        usuario.setBiografiaVectorDe(VectorDeBiografia.huellaDe(usuario.getBiografia()));
    }

    int cuantos() {
        return porCorreo.size();
    }
}
