package com.spotterai.backend.avisos;

import com.spotterai.backend.config.IdiomaConfig;
import com.spotterai.backend.models.Sesion;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.textos.Mensaje;
import com.spotterai.backend.textos.Textos;
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
 * <h2>En que idioma</h2>
 *
 * <p>En el de quien lo recibe, que sale de su columna {@code idioma}. Es el
 * unico texto del backend que no puede mirar la cabecera {@code Accept-Language}
 * de la peticion: estos correos los manda un barrido que corre cada minuto por
 * su cuenta, y cuando escribe no hay ninguna peticion. Ese es justo el motivo
 * por el que el motor escribe claves y no frases, y esta escrito en
 * {@link Mensaje}: si el backend no mandara correos, traducir podria haber sido
 * cosa del frontend.
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
    private final Textos textos;

    public RedactorDeAvisos(@Value("${spotterai.correo.url-base}") String urlBase, Textos textos) {
        // Sin la barra final, para no acabar componiendo enlaces con "//".
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        this.textos = textos;
    }

    /**
     * El idioma de quien recibe.
     *
     * <p>Lo que no se reconozca cae en español, igual que el catalogo: es el
     * idioma en el que esta escrita la aplicacion.
     */
    private Locale idiomaDe(Usuario quien) {
        return "en".equalsIgnoreCase(quien.getIdioma()) ? IdiomaConfig.INGLES : IdiomaConfig.ESPANOL;
    }

    private String enSuIdioma(Usuario quien, String clave, Object... args) {
        return textos.de(Mensaje.de(clave, args), idiomaDe(quien));
    }

    public Aviso paraSolicitud(Solicitud solicitud, String llaveDeBaja) {
        Usuario quienRecibe = solicitud.getReceptor();
        String quien = solicitud.getEmisor().getNombre();

        return new Aviso(
                quienRecibe.getEmail(),
                enSuIdioma(quienRecibe, "correo.solicitud.asunto", quien),
                enSuIdioma(quienRecibe, "correo.solicitud.cuerpo", quien, urlBase)
                        + pieDeBaja(quienRecibe, llaveDeBaja));
    }

    public Aviso paraSesion(Sesion sesion, String llaveDeBaja) {
        Usuario quienRecibe = sesion.getInvitado();
        String quien = sesion.getProponente().getNombre();
        Locale idioma = idiomaDe(quienRecibe);

        return new Aviso(
                quienRecibe.getEmail(),
                enSuIdioma(quienRecibe, "correo.sesion.asunto", quien, diaCorto(sesion, idioma)),
                enSuIdioma(quienRecibe, "correo.sesion.cuerpo",
                        quien,
                        diaLargo(sesion, idioma),
                        sesion.getHoraInicio().format(HORA),
                        sesion.getHoraFin().format(HORA),
                        donde(sesion, quienRecibe),
                        urlBase,
                        sesion.getProponente().getId())
                        + pieDeBaja(quienRecibe, llaveDeBaja));
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
                enSuIdioma(usuario, "correo.restablecer.asunto"),
                enSuIdioma(usuario, "correo.restablecer.cuerpo", urlBase, token));
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
    private String pieDeBaja(Usuario quienRecibe, String llave) {
        return enSuIdioma(quienRecibe, "correo.pieDeBaja", urlBase, llave);
    }

    /** "en McFit Madrid Centro", o nada si no consta el gimnasio. */
    private String donde(Sesion sesion, Usuario quienRecibe) {
        return sesion.getGimnasio() == null ? ""
                : enSuIdioma(quienRecibe, "correo.sesion.donde", sesion.getGimnasio().getNombre());
    }

    /**
     * "lunes 3" para el asunto, que tiene que caber.
     *
     * <p>El nombre del dia lo sabe decir Java en los dos idiomas; lo que cambia
     * entre ellos es el orden y las preposiciones, y por eso el montaje va por
     * el catalogo y no pegando cadenas.
     */
    private String diaCorto(Sesion sesion, Locale idioma) {
        return textos.de(Mensaje.de("correo.fecha.corta",
                sesion.getFecha().getDayOfWeek().getDisplayName(TextStyle.FULL, idioma),
                sesion.getFecha().getDayOfMonth()), idioma);
    }

    /** "lunes 3 de agosto" para el cuerpo, donde sí cabe el mes. */
    private String diaLargo(Sesion sesion, Locale idioma) {
        return textos.de(Mensaje.de("correo.fecha.larga",
                sesion.getFecha().getDayOfWeek().getDisplayName(TextStyle.FULL, idioma),
                sesion.getFecha().getDayOfMonth(),
                sesion.getFecha().getMonth().getDisplayName(TextStyle.FULL, idioma)), idioma);
    }
}
