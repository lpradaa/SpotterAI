package com.spotterai.backend.avisos;

/**
 * Lo que sabe poner un correo en la calle.
 *
 * <p>Es una interfaz y no una llamada directa a {@code JavaMailSender} para que
 * la aplicacion pueda arrancar y funcionar entera sin un servidor de correo
 * configurado. En desarrollo no hay SMTP, y un backend que no arranca por eso
 * —o peor, que revienta al aceptar una solicitud— es un precio que no hay
 * ninguna razon para pagar.
 *
 * <p>Hay dos implementaciones y las elige la configuracion:
 * {@link CarteroSmtp} cuando {@code spotterai.correo.activo} es true, y
 * {@link CarteroAlRegistro} en cualquier otro caso.
 */
public interface Cartero {

    /**
     * Manda el aviso.
     *
     * @throws RuntimeException si no se ha podido entregar. Quien llame decide
     *         que hacer con eso; lo que no puede es dejar el aviso por mandado.
     */
    void enviar(Aviso aviso);
}
