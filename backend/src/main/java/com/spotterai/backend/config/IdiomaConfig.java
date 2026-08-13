package com.spotterai.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * En que idioma responde el backend.
 *
 * <p>Sale de la cabecera {@code Accept-Language}, que el frontend rellena con la
 * eleccion explicita de quien usa la aplicacion y no con la del navegador: si
 * alguien con el sistema en ingles ha pulsado "español", eso es lo que quiere
 * leer.
 *
 * <p>Se declaran los idiomas admitidos a proposito. Sin la lista,
 * {@code AcceptHeaderLocaleResolver} acepta cualquier cosa que llegue en la
 * cabecera y luego {@code MessageSource} cae al fichero por defecto, lo cual
 * funciona pero deja el {@code Locale} del hilo en, por ejemplo, aleman — y ese
 * mismo {@code Locale} lo usan los formatos de fecha y numero. Con la lista, lo
 * que no conocemos entra directamente como español.
 *
 * <p>Español es el idioma por defecto porque es en el que esta escrita la
 * aplicacion: si un dia falta una traduccion inglesa, lo que se ve es el texto
 * original y no una clave.
 */
@Configuration
public class IdiomaConfig {

    public static final Locale ESPANOL = Locale.forLanguageTag("es");
    public static final Locale INGLES = Locale.forLanguageTag("en");

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolutor = new AcceptHeaderLocaleResolver();
        resolutor.setSupportedLocales(List.of(ESPANOL, INGLES));
        resolutor.setDefaultLocale(ESPANOL);
        return resolutor;
    }
}
