package com.spotterai.backend.avisos;

/**
 * Un correo ya redactado, listo para salir.
 *
 * <p>Texto plano y no HTML a proposito. Un aviso de esta aplicacion dice una
 * frase y da un enlace; el HTML añadiria una plantilla que mantener, imagenes
 * que no cargan en la mitad de los clientes y una version en texto que hay que
 * escribir igualmente porque muchos la prefieren.
 *
 * @param para    direccion del destinatario
 * @param asunto  lo que se lee sin abrir el correo, que es donde se decide si se abre
 * @param cuerpo  el texto, con el enlace dentro
 */
public record Aviso(String para, String asunto, String cuerpo) {
}
