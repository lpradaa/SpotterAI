package com.spotterai.backend.textos;

import com.spotterai.backend.config.IdiomaConfig;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Un {@link Textos} de verdad para las pruebas que no levantan Spring.
 *
 * <p>Con el catalogo real y no con un doble, a proposito: la mayoria de las
 * pruebas del motor afirman sobre la frase que sale —"Los dos vais siempre 2
 * dias a la misma hora (Lunes y Miercoles)"— y esa afirmacion es justo lo que
 * protege la prosa del producto. Con un doble que devolviera la clave, esas
 * pruebas seguirian en verde diciendo mucho menos, y ademas dejarian de avisar
 * si una clave se queda sin texto.
 *
 * <p>Fija ademas el idioma del hilo en español, porque fuera de una peticion web
 * no hay cabecera que mirar y el que hubiera quedado de otra prueba se colaria
 * aqui.
 */
public final class TextosDePrueba {

    private TextosDePrueba() {}

    public static Textos nuevo() {
        LocaleContextHolder.setLocale(IdiomaConfig.ESPANOL);

        ResourceBundleMessageSource fuente = new ResourceBundleMessageSource();
        fuente.setBasename("messages");
        fuente.setDefaultEncoding("UTF-8");
        fuente.setFallbackToSystemLocale(false);

        return new Textos(fuente);
    }

    /**
     * Un {@link Mensaje} que se redacta como el texto que se le da.
     *
     * <p>Para las pruebas que fabrican factores a mano: lo que se comprueba ahi
     * es como se hilan los detalles —el orden, el punto entre frases, que un
     * factor a cero no entre—, no de donde sale cada frase. Inventar una clave de
     * catalogo por cada texto de prueba solo añadiria ruido al catalogo de
     * verdad.
     */
    public static Mensaje literal(String texto) {
        return Mensaje.de("lista.tal_cual", texto);
    }
}
