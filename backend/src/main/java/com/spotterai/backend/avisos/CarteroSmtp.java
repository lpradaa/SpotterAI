package com.spotterai.backend.avisos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * El que manda correo de verdad.
 *
 * <p>Solo existe como bean si {@code spotterai.correo.activo} es true. La
 * condicion no es un interruptor de comodidad: sin ella, un despliegue sin SMTP
 * configurado tendria un bean que falla en cada envio, y el fallo apareceria en
 * el barrido cada minuto para siempre.
 *
 * <p>No captura las excepciones. Que un correo no salga es informacion que el
 * que llama necesita para no dar el aviso por mandado y volver a intentarlo.
 */
@Component
@ConditionalOnProperty(name = "spotterai.correo.activo", havingValue = "true")
public class CarteroSmtp implements Cartero {

    private final JavaMailSender correo;
    private final String remitente;

    public CarteroSmtp(JavaMailSender correo,
                       @Value("${spotterai.correo.remitente}") String remitente) {
        this.correo = correo;
        this.remitente = remitente;
    }

    @Override
    public void enviar(Aviso aviso) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(aviso.para());
        mensaje.setSubject(aviso.asunto());
        mensaje.setText(aviso.cuerpo());

        correo.send(mensaje);
    }
}
