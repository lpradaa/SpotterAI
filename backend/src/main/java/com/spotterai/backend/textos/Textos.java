package com.spotterai.backend.textos;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Compone un {@link Mensaje} en el idioma de quien ha preguntado.
 *
 * <p>Es el unico sitio de la aplicacion donde una clave se convierte en una
 * frase. Todo lo que hay por encima —las calculadoras, los servicios— trabaja
 * con claves, que es lo que permite que la misma explicacion salga en dos
 * idiomas sin duplicar la logica que la decide.
 *
 * <p>El idioma sale de {@code LocaleContextHolder}, que en una peticion web lo
 * ha puesto el resolutor a partir de la cabecera {@code Accept-Language} que
 * manda el frontend. Fuera de una peticion —un correo que se manda desde una
 * tarea— cae al idioma por defecto, y por eso los metodos que necesitan un
 * idioma concreto lo pueden recibir.
 */
@Service
public class Textos {

    private final MessageSource fuente;

    public Textos(MessageSource fuente) {
        this.fuente = fuente;
    }

    /** En el idioma de la peticion en curso. */
    public String de(Mensaje mensaje) {
        return de(mensaje, LocaleContextHolder.getLocale());
    }

    /**
     * En un idioma concreto.
     *
     * <p>Los argumentos se componen primero, y en el mismo idioma: un mensaje
     * puede llevar otros dentro —"Coincidiriais {0} a la semana", donde {0} es
     * "dos dias"— y sin esto la frase de fuera saldria traducida con las piezas
     * de dentro en español. Es el fallo tipico al traducir texto compuesto, y
     * aqui era seguro que pasaba: los nombres de los dias y los plurales de la
     * calculadora son exactamente eso.
     */
    public String de(Mensaje mensaje, Locale idioma) {
        Object[] args = mensaje.args();
        Object[] compuestos = new Object[args.length];

        for (int i = 0; i < args.length; i++) {
            compuestos[i] = args[i] instanceof Mensaje anidado ? de(anidado, idioma) : args[i];
        }

        return fuente.getMessage(mensaje.clave(), compuestos, idioma);
    }
}
