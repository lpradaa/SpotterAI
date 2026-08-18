package com.spotterai.backend.semantica;

import com.spotterai.backend.models.Usuario;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Mantiene al dia lo que la biografia de alguien dice sobre como quiere entrenar.
 *
 * <p>Hermano de {@link VectorDeBiografia} y con el mismo reparto de
 * responsabilidades: decide <em>cuando</em> hay que releer la biografia, que es
 * donde estan las dos trampas —releer siempre gasta una llamada de red en cada
 * guardado aunque el texto no haya cambiado, y no releer nunca deja tres numeros
 * describiendo un texto que ya no existe.
 *
 * <p>La huella resuelve las dos, igual que con el vector: es el resumen del
 * texto del que salieron los ejes.
 *
 * <h2>Por que sustituye al vector</h2>
 *
 * <p>Porque el vector media otra cosa. Comparar dos biografias con la similitud
 * del coseno resulto ordenar por <b>parecido de redaccion</b>: dos personas que
 * querian lo contrario dicho con la misma estructura sacaban 0,843 y dos que
 * querian lo mismo dicho con sus palabras, 0,499. Y no se arreglaba con otro
 * modelo de la misma clase, porque la oposicion entre dos frases no es propiedad
 * de ninguna de las dos. Todo el recorrido esta en {@code docs/medir-el-motor.md}.
 */
@Service
public class IntencionesDeBiografia {

    /**
     * Los ejes que se guardan, y el orden no importa: cada uno va a su columna.
     *
     * <p>Estan aqui y en {@code embeddings/intenciones.py}, que es la clase de
     * duplicado que diverge. Si el servicio empieza a mandar un eje que esto no
     * conoce, se ignora en silencio y el factor se queda con menos informacion
     * sin que nadie se entere — por eso hay una prueba que fija los tres nombres.
     */
    public static final String EXIGENCIA = "exigencia";
    public static final String AMBICION = "ambicion";
    public static final String FLEXIBILIDAD = "flexibilidad";

    private final ServicioDeEmbeddings servicio;

    public IntencionesDeBiografia(ServicioDeEmbeddings servicio) {
        this.servicio = servicio;
    }

    /**
     * Pone al dia los ejes del usuario si hace falta.
     *
     * <p>No guarda: modifica la entidad y deja que la escriba quien la tenia,
     * dentro de la transaccion que ya estaba abierta.
     */
    public boolean actualizar(Usuario usuario) {
        String bio = usuario.getBiografia();

        // Sin biografia no hay intenciones. Y si las habia —alguien borro su
        // bio— hay que quitarlas: dejarlas seria seguir emparejando a esa
        // persona por un texto que ha decidido retirar.
        if (bio == null || bio.isBlank()) {
            if (usuario.getIntencionesDe() == null) return false;
            escribir(usuario, null, null, null, null);
            return true;
        }

        return servicio.leerIntenciones(bio)
                .filter(i -> !i.huella().equals(usuario.getIntencionesDe()))
                .map(i -> {
                    Map<String, Double> ejes = i.ejes();
                    escribir(usuario,
                            ejes.get(EXIGENCIA), ejes.get(AMBICION), ejes.get(FLEXIBILIDAD),
                            i.huella());
                    return true;
                })
                .orElse(false);
    }

    /** Si lo guardado corresponde a la biografia de ahora. */
    public boolean estaAlDia(Usuario usuario) {
        String bio = usuario.getBiografia();

        if (bio == null || bio.isBlank()) return usuario.getIntencionesDe() == null;
        return huellaDe(bio).equals(usuario.getIntencionesDe());
    }

    /**
     * La misma huella que calcula el servicio al leer el texto.
     *
     * <p>Son dos implementaciones de la misma especificacion —los primeros 32
     * caracteres del SHA-256 del texto recortado— y eso es exactamente lo que
     * suele divergir, asi que hay una prueba que fija el valor para un texto
     * conocido y falla si alguno de los dos lados se mueve.
     */
    public static String huellaDe(String texto) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(texto.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 tiene que estar disponible", e);
        }
    }

    private static void escribir(Usuario u, Double exigencia, Double ambicion,
                                 Double flexibilidad, String huella) {
        u.setIntencionExigencia(exigencia);
        u.setIntencionAmbicion(ambicion);
        u.setIntencionFlexibilidad(flexibilidad);
        u.setIntencionesDe(huella);
    }
}
