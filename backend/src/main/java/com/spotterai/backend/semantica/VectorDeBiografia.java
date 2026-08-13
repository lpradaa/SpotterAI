package com.spotterai.backend.semantica;

import com.spotterai.backend.models.Usuario;
import org.springframework.stereotype.Service;

/**
 * Mantiene al dia el vector de la biografia de una persona.
 *
 * <p>Es la pieza que decide <em>cuando</em> hay que recalcular, que es donde
 * estan las dos trampas: recalcular siempre gasta una llamada de red en cada
 * guardado de perfil aunque la biografia no haya cambiado, y no recalcular nunca
 * deja vectores describiendo textos que ya no existen.
 *
 * <p>La huella resuelve las dos. Es el resumen del texto del que salio el
 * vector: si coincide con el texto actual, el vector sigue siendo valido y no se
 * llama a nadie; si no coincide —o no hay— se recalcula.
 *
 * <p>La calculan los dos lados: el servicio de embeddings al vectorizar, y
 * {@link #huellaDe} aqui, para poder detectar un vector desfasado sin gastar una
 * llamada. Son dos implementaciones de la misma especificacion y eso es
 * exactamente lo que suele divergir, asi que hay una prueba que fija el valor
 * para un texto conocido y falla si alguno de los dos se mueve.
 */
@Service
public class VectorDeBiografia {

    private final ServicioDeEmbeddings embeddings;

    public VectorDeBiografia(ServicioDeEmbeddings embeddings) {
        this.embeddings = embeddings;
    }

    /**
     * Pone al dia el vector del usuario si hace falta.
     *
     * <p>No guarda: modifica la entidad y deja que la escriba quien la tenia,
     * dentro de la transaccion que ya estaba abierta. Devuelve si ha cambiado
     * algo, por si quien llama quiere saberlo.
     */
    public boolean actualizar(Usuario usuario) {
        String bio = usuario.getBiografia();

        // Sin biografia no hay vector. Y si habia uno —alguien borro su bio—
        // hay que quitarlo: dejarlo seria seguir comparando a esa persona por un
        // texto que ha decidido retirar.
        if (bio == null || bio.isBlank()) {
            if (usuario.getBiografiaVector() == null) return false;
            usuario.setBiografiaVector(null);
            usuario.setBiografiaVectorDe(null);
            return true;
        }

        return embeddings.vectorizar(bio)
                .filter(v -> !v.huella().equals(usuario.getBiografiaVectorDe()))
                .map(v -> {
                    usuario.setBiografiaVector(VectorDeTexto.de(v.vector()).aBytes());
                    usuario.setBiografiaVectorDe(v.huella());
                    return true;
                })
                .orElse(false);
    }

    /**
     * Si el vector que tiene esta persona corresponde a su biografia actual.
     *
     * <p>Lo usa el repaso de arranque para saber a quien le falta trabajo sin
     * llamar al modelo por cada uno.
     *
     * <p>La primera version solo miraba que el vector no fuera nulo, y eso dejaba
     * un agujero que aparecio al probar la degradacion: alguien edita su
     * biografia con el servicio de embeddings caido, el perfil se guarda con el
     * vector viejo, y el repaso del siguiente arranque lo daba por al dia. Ese
     * vector se quedaba describiendo un texto que ya no existe, para siempre.
     * Comparando la huella se detecta.
     */
    public boolean estaAlDia(Usuario usuario) {
        String bio = usuario.getBiografia();
        if (bio == null || bio.isBlank()) return usuario.getBiografiaVector() == null;

        return usuario.getBiografiaVector() != null
                && huellaDe(bio).equals(usuario.getBiografiaVectorDe());
    }

    /**
     * El resumen del texto del que salio un vector.
     *
     * <p>Tiene que dar exactamente lo mismo que calcula el servicio de
     * embeddings —SHA-256 del texto sin espacios alrededor, en hexadecimal, los
     * 32 primeros caracteres—. Son dos implementaciones de la misma
     * especificacion, que es justo lo que suele divergir: hay una prueba que fija
     * el valor para un texto conocido y falla si alguna de las dos se mueve.
     */
    public static String huellaDe(String texto) {
        try {
            byte[] resumen = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(texto.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : resumen) hex.append("%02x".formatted(b));
            return hex.substring(0, 32);

        } catch (java.security.NoSuchAlgorithmException e) {
            // SHA-256 es obligatorio en toda maquina virtual de Java desde
            // siempre. Si falta, el problema no es este metodo.
            throw new IllegalStateException(e);
        }
    }
}
