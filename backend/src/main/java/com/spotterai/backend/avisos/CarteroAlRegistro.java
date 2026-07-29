package com.spotterai.backend.avisos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * El que no manda nada y lo escribe en el registro.
 *
 * <p>Es el que hay cuando no se ha configurado un servidor de correo, que es el
 * caso en desarrollo y en cualquier arranque recien clonado. Escribe el aviso
 * entero —destinatario, asunto y cuerpo— en vez de tragarselo en silencio: lo
 * que se quiere poder responder sin SMTP es "¿se habria mandado el correo, a
 * quien, y que ponia?", y un contador de envios no responde a eso.
 *
 * <p>No es un doble de pruebas. Es el comportamiento normal de una instalacion
 * sin correo, y por eso vive en el codigo de produccion y no en los tests.
 *
 * <p>La condicion es el espejo exacto de la de {@link CarteroSmtp} y no un
 * {@code @ConditionalOnMissingBean}: esa se evalua segun el orden en que se
 * registran los beans, que al escanear componentes no esta garantizado, y el
 * fallo seria dos carteros o ninguno segun el dia. Asi hay exactamente uno, y
 * cual es se lee en una linea de configuracion.
 */
@Component
@ConditionalOnProperty(name = "spotterai.correo.activo", havingValue = "false", matchIfMissing = true)
public class CarteroAlRegistro implements Cartero {

    private static final Logger log = LoggerFactory.getLogger(CarteroAlRegistro.class);

    @Override
    public void enviar(Aviso aviso) {
        log.info("""
                Aviso por correo NO enviado (spotterai.correo.activo=false)
                  Para:   {}
                  Asunto: {}
                {}""", aviso.para(), aviso.asunto(), aviso.cuerpo());
    }
}
