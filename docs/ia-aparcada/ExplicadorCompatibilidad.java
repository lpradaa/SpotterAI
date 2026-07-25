package com.spotterai.backend.matching;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Convierte una puntuacion de compatibilidad en una explicacion que una persona
 * quiera leer.
 *
 * <p><strong>El modelo no puntua.</strong> Recibe el desglose ya calculado por
 * {@link CalculadoraCompatibilidad} y solo lo redacta. Asi la puntuacion sigue
 * siendo reproducible y auditable, y la explicacion nunca puede contradecirla.
 *
 * <p>Si no hay clave de API configurada o la llamada falla, se devuelve una
 * explicacion construida con los textos deterministas de cada factor: la
 * aplicacion funciona igual, solo con una redaccion mas seca.
 */
@Service
public class ExplicadorCompatibilidad {

    private static final Logger log = LoggerFactory.getLogger(ExplicadorCompatibilidad.class);

    private static final String MODELO = "claude-opus-5";

    private static final String INSTRUCCIONES = """
            Eres el motor de recomendaciones de SpotterAI, una aplicacion que empareja
            companeros de entrenamiento de gimnasio.

            Recibes el desglose ya calculado de la compatibilidad entre dos personas y
            lo conviertes en una explicacion breve para quien recibe la sugerencia.

            Reglas:
            - No inventes datos. Usa unicamente los factores que se te pasan.
            - No cuestiones ni recalcules la puntuacion: tu trabajo es explicarla.
            - Si hay coincidencia de horarios, mencionala siempre y con los dias concretos:
              es el dato que decide si dos personas pueden entrenar juntas.
            - Si la compatibilidad es baja, dilo con naturalidad en vez de adornarla.
            - Escribe en espanol de Espana, en segunda persona del plural, sin emojis
              y sin superlativos vacios.
            """;

    private final String apiKey;
    private volatile AnthropicClient cliente;

    public ExplicadorCompatibilidad(@Value("${ANTHROPIC_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;
    }

    public ExplicacionMatch explicar(String nombreOtro, PuntuacionCompatibilidad puntuacion) {
        AnthropicClient client = obtenerCliente();
        if (client == null) {
            return explicacionDeRespaldo(nombreOtro, puntuacion);
        }

        try {
            StructuredMessageCreateParams<ExplicacionMatch> params = MessageCreateParams.builder()
                    .model(MODELO)
                    // Amplio a proposito: en este modelo el razonamiento va dentro del mismo
                    // limite que la respuesta, y quedarse corto trunca la salida a medias.
                    .maxTokens(16000L)
                    .system(INSTRUCCIONES)
                    .outputConfig(ExplicacionMatch.class)
                    .addUserMessage(construirResumen(nombreOtro, puntuacion))
                    .build();

            return client.messages().create(params).content().stream()
                    .flatMap(bloque -> bloque.text().stream())
                    .map(bloque -> bloque.text())
                    .findFirst()
                    .orElseGet(() -> explicacionDeRespaldo(nombreOtro, puntuacion));

        } catch (RuntimeException e) {
            // Una sugerencia sin redactar es un problema menor; dejar la pantalla en
            // blanco no lo es. Se registra y se sigue con el texto determinista.
            log.warn("No se pudo redactar la explicacion del match con {}: {}",
                    nombreOtro, e.getMessage());
            return explicacionDeRespaldo(nombreOtro, puntuacion);
        }
    }

    /** Empaqueta el desglose como texto plano para el modelo. */
    String construirResumen(String nombreOtro, PuntuacionCompatibilidad puntuacion) {
        String factores = puntuacion.factores().stream()
                .map(f -> "- %s: %.0f de %.0f puntos. %s".formatted(
                        f.nombre(), f.puntos(), f.puntosMax(), f.detalle()))
                .collect(Collectors.joining("\n"));

        String franjas = puntuacion.solape().hayCoincidencia()
                ? "\nFranjas exactas en comun: " + puntuacion.solape().franjas().stream()
                        .map(FranjaComun::describir)
                        .collect(Collectors.joining(", "))
                : "\nNo hay ninguna franja horaria en comun.";

        return """
                Companero sugerido: %s
                Puntuacion total: %d sobre 100 (%s)

                Desglose:
                %s%s
                """.formatted(nombreOtro, puntuacion.total(), puntuacion.etiqueta(), factores, franjas);
    }

    /**
     * Explicacion sin IA, hilando los textos que ya trae cada factor. Cubre el caso
     * de despliegue sin clave y el de fallo de la API.
     */
    ExplicacionMatch explicacionDeRespaldo(String nombreOtro, PuntuacionCompatibilidad puntuacion) {
        String motivo = puntuacion.factores().stream()
                .filter(f -> f.puntos() > 0)
                .map(FactorCompatibilidad::detalle)
                .collect(Collectors.joining(". "));

        if (motivo.isBlank()) {
            motivo = "No hemos encontrado puntos en común claros entre vuestros perfiles.";
        } else {
            motivo += ".";
        }

        return new ExplicacionMatch("%s - %d%% de compatibilidad".formatted(nombreOtro, puntuacion.total()), motivo);
    }

    /** Crea el cliente la primera vez que hace falta. Devuelve null si no hay clave. */
    private AnthropicClient obtenerCliente() {
        if (apiKey == null || apiKey.isBlank()) return null;
        AnthropicClient local = cliente;
        if (local == null) {
            synchronized (this) {
                local = cliente;
                if (local == null) {
                    local = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
                    cliente = local;
                }
            }
        }
        return local;
    }
}
