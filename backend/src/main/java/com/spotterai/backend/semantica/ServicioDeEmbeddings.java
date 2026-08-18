package com.spotterai.backend.semantica;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Habla con el servicio que convierte una biografia en un vector.
 *
 * <p><b>Nada de esto esta en el camino critico.</b> Un embedding se calcula
 * cuando alguien guarda su perfil, no cuando se emparejan dos personas:
 * emparejar es un producto escalar sobre vectores ya guardados. Por eso este
 * servicio puede estar caido —o no estar desplegado— sin que el motor pierda
 * nada de velocidad ni deje de responder.
 *
 * <p>Cuando falla, devuelve {@link Optional#empty()} y ya esta. No relanza la
 * excepcion ni reintenta: la biografia se queda sin vector, el factor semantico
 * queda "sin datos" para esa persona, y la calculadora reparte su peso entre los
 * demas factores exactamente igual que hace con un perfil sin gimnasio. Un
 * servicio de modelo caido degrada el producto; no puede romperlo.
 */
@Service
public class ServicioDeEmbeddings {

    private static final Logger log = LoggerFactory.getLogger(ServicioDeEmbeddings.class);

    /**
     * Timeouts cortos a proposito.
     *
     * <p>Esto corre dentro de "guardar mi perfil", que es una peticion que una
     * persona esta esperando. Mas vale guardar el perfil sin vector —y
     * recalcularlo en la siguiente edicion— que tener a alguien mirando una
     * ruedecita porque un modelo esta tardando en cargar.
     */
    private static final Duration ESPERA_CONEXION = Duration.ofSeconds(2);
    private static final Duration ESPERA_RESPUESTA = Duration.ofSeconds(5);

    private final RestClient cliente;
    private final boolean activo;

    public ServicioDeEmbeddings(@Value("${spotterai.embeddings.url:}") String url) {
        this.activo = url != null && !url.isBlank();

        if (!activo) {
            // Ni un warning: no tener el servicio desplegado es una
            // configuracion valida, no un fallo. En desarrollo, en las pruebas y
            // en cualquier despliegue que no lo levante, la aplicacion funciona
            // entera sin el factor semantico.
            log.info("Sin servicio de embeddings configurado: el factor semántico quedará sin datos.");
            this.cliente = null;
            return;
        }

        var fabrica = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout((int) ESPERA_CONEXION.toMillis());
        fabrica.setReadTimeout((int) ESPERA_RESPUESTA.toMillis());

        this.cliente = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();

        log.info("Servicio de embeddings en {}", url);
    }

    /** Lo que responde el servicio. */
    public record Vectorizado(List<Double> vector, String huella, int dimensiones) {}

    /**
     * Lo que dice una biografia, leido por ejes.
     *
     * @param ejes  nombre del eje -> posicion de -1 a 1, o <b>null</b> si el
     *              texto no habla de ese eje. El null viaja tal cual a la base:
     *              es un dato ("no ha dicho nada de esto") y no un fallo.
     * @param huella de que texto salieron
     */
    public record Intenciones(Map<String, Double> ejes, String huella) {}

    /**
     * Los ejes de un texto, si se puede.
     *
     * <p>Mismo contrato que {@link #vectorizar}: vacio cuando no hay servicio,
     * el texto esta en blanco o el servicio falla. La calculadora ya sabe
     * puntuar sin este factor, asi que un servicio caido no rompe nada — solo
     * deja de releer las biografias que cambien mientras tanto.
     */
    public Optional<Intenciones> leerIntenciones(String texto) {
        if (!activo || texto == null || texto.isBlank()) return Optional.empty();

        try {
            Intenciones respuesta = cliente.post()
                    .uri("/intenciones")
                    .body(new PeticionDeVector(texto.trim()))
                    .retrieve()
                    .body(Intenciones.class);

            if (respuesta == null || respuesta.ejes() == null) return Optional.empty();
            return Optional.of(respuesta);

        } catch (Exception e) {
            // Mismo criterio que arriba: esto es opcional por diseño y no puede
            // tumbar un guardado de perfil.
            log.warn("No se han podido leer las intenciones: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * El vector de un texto, si se puede.
     *
     * @return vacio cuando no hay servicio configurado, el texto esta en blanco,
     *         el servicio falla, o responde un vector de una dimension que no
     *         reconocemos
     */
    public Optional<Vectorizado> vectorizar(String texto) {
        if (!activo || texto == null || texto.isBlank()) return Optional.empty();

        try {
            Vectorizado respuesta = cliente.post()
                    .uri("/vector")
                    .body(new PeticionDeVector(texto.trim()))
                    .retrieve()
                    .body(Vectorizado.class);

            if (respuesta == null || respuesta.vector() == null) return Optional.empty();

            // Un vector de otra dimension significa que el servicio esta
            // sirviendo un modelo distinto del que espera la base. Guardarlo
            // seria mezclar dos espacios vectoriales en la misma columna, y las
            // similitudes resultantes no significarian nada.
            if (respuesta.dimensiones() != VectorDeTexto.DIMENSIONES) {
                log.warn("El servicio de embeddings devuelve {} dimensiones y esperábamos {}: se descarta",
                        respuesta.dimensiones(), VectorDeTexto.DIMENSIONES);
                return Optional.empty();
            }

            return Optional.of(respuesta);

        } catch (Exception e) {
            // A nivel de aviso y no de error: que este servicio no responda no
            // es una averia de SpotterAI, es una funcion de mas que hoy no esta.
            log.warn("No se ha podido vectorizar la biografía ({}): se guarda sin vector", e.getMessage());
            return Optional.empty();
        }
    }

    private record PeticionDeVector(String texto) {}
}
