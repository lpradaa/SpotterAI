package com.spotterai.backend.avisos;

import com.spotterai.backend.models.Sesion;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Escribe lo que va a leer la persona.
 *
 * <p>Separado del envio y sin tocar nada de fuera: asi se puede comprobar lo
 * que dicen los correos sin levantar un servidor ni la base. Lo que se rompe en
 * un aviso no es el SMTP, es la frase —el nombre que falta, la fecha en formato
 * ISO, el enlace que apunta a localhost en produccion—.
 *
 * <h2>Por que estos correos dicen lo que dicen</h2>
 *
 * <p>El asunto lleva el nombre de la persona y lo que pide, porque en la bandeja
 * de entrada se decide si se abre con esa linea sola. "Tienes una notificacion"
 * no dice si merece la pena.
 *
 * <p>El cuerpo dice quien, que y cuando, y da un enlace directo a la pantalla
 * donde se responde. Nada mas: quien recibe esto ya sabe lo que es SpotterAI, y
 * cada parrafo de mas es una razon para no leer el que importa.
 *
 * <p>No se manda nunca la compatibilidad ni ningun dato del perfil del otro. Un
 * correo se reenvia, se lee en una pantalla compartida y se queda en servidores
 * que no son nuestros; que a alguien le llegue "Marta, 93 % contigo" es contar
 * de Marta algo que Marta no ha decidido contar.
 */
@Component
public class RedactorDeAvisos {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final String urlBase;

    public RedactorDeAvisos(@Value("${spotterai.correo.url-base}") String urlBase) {
        // Sin la barra final, para no acabar componiendo enlaces con "//".
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    public Aviso paraSolicitud(Solicitud solicitud, String llaveDeBaja) {
        String quien = solicitud.getEmisor().getNombre();

        return new Aviso(
                solicitud.getReceptor().getEmail(),
                quien + " quiere entrenar contigo",
                """
                %s te ha mandado una solicitud en SpotterAI.

                Puedes aceptarla o rechazarla aquí:
                %s/solicitudes

                Si no respondes no pasa nada: la solicitud se queda esperando.
                %s""".formatted(quien, urlBase, pieDeBaja(llaveDeBaja)));
    }

    public Aviso paraSesion(Sesion sesion, String llaveDeBaja) {
        String quien = sesion.getProponente().getNombre();

        return new Aviso(
                sesion.getInvitado().getEmail(),
                quien + " propone entrenar el " + diaCorto(sesion),
                """
                %s propone que entrenéis juntos:

                  %s, de %s a %s%s

                Responde aquí:
                %s/conexiones?con=%d

                Si ese día no te viene bien, dilo y proponéis otro.
                %s""".formatted(
                        quien,
                        diaLargo(sesion),
                        sesion.getHoraInicio().format(HORA),
                        sesion.getHoraFin().format(HORA),
                        donde(sesion),
                        urlBase,
                        sesion.getProponente().getId(),
                        pieDeBaja(llaveDeBaja)));
    }

    /**
     * El correo para recuperar la contraseña.
     *
     * <p>Sin pie de baja, y no es un olvido: esto no es un aviso del que uno
     * pueda darse de baja, es la respuesta a algo que la persona acaba de pedir.
     * Quitarle la posibilidad de recibirlo la dejaria sin forma de recuperar su
     * cuenta.
     *
     * <p>Dice cuanto dura y que hacer si no fue uno quien lo pidio. Lo segundo
     * importa: recibir esto sin haberlo pedido significa que alguien esta
     * intentando entrar, y la persona merece saber que no tiene que hacer nada.
     */
    public Aviso paraRestablecer(Usuario usuario, String token) {
        return new Aviso(
                usuario.getEmail(),
                "Recuperar tu contraseña de SpotterAI",
                """
                Has pedido cambiar tu contraseña. Puedes hacerlo aquí:

                %s/restablecer?t=%s

                El enlace vale una hora y solo se puede usar una vez.

                Si no has sido tú, no hace falta que hagas nada: mientras no
                abras ese enlace, tu contraseña sigue siendo la de siempre.
                """.formatted(urlBase, token));
    }

    /**
     * La salida, al final de todos los avisos.
     *
     * <p>Va en todos y no solo en el primero: el correo que alguien abre cuando
     * se harta es el que tiene delante, no el de hace tres semanas, y si ese no
     * trae la salida lo que hace es marcarlo como no deseado. A partir de ahi
     * tampoco llegan los que si queria.
     *
     * <p>El enlace lleva a una pantalla con un boton y no da de baja al abrirlo.
     * Los antivirus de correo y los previsualizadores siguen los enlaces por su
     * cuenta: con una baja de un solo clic, media lista se da de baja sola sin
     * que nadie haya tocado nada.
     */
    private String pieDeBaja(String llave) {
        return """

                --
                ¿No quieres estos avisos? Páralos aquí:
                %s/baja?t=%s
                """.formatted(urlBase, llave);
    }

    /** "en McFit Madrid Centro", o nada si no consta el gimnasio. */
    private String donde(Sesion sesion) {
        return sesion.getGimnasio() == null ? "" : ", en " + sesion.getGimnasio().getNombre();
    }

    /** "lunes 3" para el asunto, que tiene que caber. */
    private String diaCorto(Sesion sesion) {
        return sesion.getFecha().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.of("es", "ES"))
                + " " + sesion.getFecha().getDayOfMonth();
    }

    /** "lunes 3 de agosto" para el cuerpo, donde sí cabe el mes. */
    private String diaLargo(Sesion sesion) {
        Locale es = Locale.of("es", "ES");
        return sesion.getFecha().getDayOfWeek().getDisplayName(TextStyle.FULL, es)
                + " " + sesion.getFecha().getDayOfMonth()
                + " de " + sesion.getFecha().getMonth().getDisplayName(TextStyle.FULL, es);
    }
}
