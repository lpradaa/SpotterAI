package com.spotterai.backend.textos;

import com.spotterai.backend.config.IdiomaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Que el backend hable dos idiomas.
 *
 * <p>Lo que se fija aqui no es que exista un fichero de traducciones, es lo que
 * de verdad se rompe al traducir prosa generada: <b>los mensajes que van dentro
 * de otros mensajes</b>. "Los dos vais siempre {0} a la misma hora ({1})" se
 * compone de piezas —"dos dias", "Lunes y Miercoles"— que tambien son texto. Si
 * solo se traduce la frase de fuera, sale una frase inglesa con las tripas en
 * español y nadie se entera hasta que lo ve un usuario.
 */
@SpringBootTest
class TextosTest {

    @Autowired
    private Textos textos;

    @Test
    @DisplayName("la misma clave da dos frases distintas")
    void traduceSegunElIdioma() {
        Mensaje sinSolape = Mensaje.de("factor.horario.sinSolape");

        assertThat(textos.de(sinSolape, IdiomaConfig.ESPANOL))
                .isEqualTo("Vuestros horarios no coinciden en ningún momento de la semana");
        assertThat(textos.de(sinSolape, IdiomaConfig.INGLES))
                .isEqualTo("Your schedules do not overlap at any point in the week");
    }

    @Test
    @DisplayName("los mensajes de dentro se traducen tambien")
    void traduceLoQueVaDentro() {
        // Esto es el caso real de la calculadora: la frase lleva dentro un
        // recuento de dias y una lista de nombres de dias, y las dos piezas son
        // texto que cambia de idioma.
        Mensaje dias = Mensaje.de("comun.dias.varios", 2);
        Mensaje cuales = Mensaje.de("lista.dos",
                Mensaje.de("dia.LUNES"), Mensaje.de("dia.MIERCOLES"));
        Mensaje frase = Mensaje.de("factor.horario.ambosFijos", dias, cuales);

        assertThat(textos.de(frase, IdiomaConfig.ESPANOL))
                .isEqualTo("Los dos vais siempre 2 días a la misma hora (Lunes y Miércoles)");
        assertThat(textos.de(frase, IdiomaConfig.INGLES))
                .isEqualTo("You both always train 2 days at the same time (Monday and Wednesday)");
    }

    @Test
    @DisplayName("una pieza suelta tambien cambia")
    void traduceLasPiezasSueltas() {
        assertThat(textos.de(Mensaje.de("comun.dias.uno"), IdiomaConfig.ESPANOL)).isEqualTo("un día");
        assertThat(textos.de(Mensaje.de("comun.dias.uno"), IdiomaConfig.INGLES)).isEqualTo("one day");
    }

    @Test
    @DisplayName("un numero dentro de la frase no se toca")
    void losNumerosNoSeTraducen() {
        Mensaje titular = Mensaje.de("compat.titular", "Marta", 93);

        assertThat(textos.de(titular, IdiomaConfig.ESPANOL)).isEqualTo("Marta - 93% de compatibilidad");
        assertThat(textos.de(titular, IdiomaConfig.INGLES)).isEqualTo("Marta - 93% compatible");
    }

    @Test
    @DisplayName("sin idioma en el hilo se responde en español")
    void porDefectoEspanol() {
        // Fuera de una peticion web —un correo lanzado desde una tarea— no hay
        // cabecera que mirar. Que caiga en español y no en el idioma del
        // servidor, que es lo que pasaria dejandolo al sistema.
        LocaleContextHolder.setLocale(null);

        assertThat(textos.de(Mensaje.de("compat.etiqueta.excelente")))
                .isEqualTo("Compatibilidad excelente");
    }

    @Test
    @DisplayName("un idioma que no conocemos cae en español, no en una clave")
    void idiomaDesconocido() {
        assertThat(textos.de(Mensaje.de("compat.etiqueta.excelente"), Locale.forLanguageTag("de")))
                .isEqualTo("Compatibilidad excelente");
    }
}
