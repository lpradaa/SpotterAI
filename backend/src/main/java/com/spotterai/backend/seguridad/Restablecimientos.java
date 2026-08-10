package com.spotterai.backend.seguridad;

import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Recuperar una contraseña olvidada.
 *
 * <p>Hasta ahora no habia forma: quien la olvidaba se quedaba fuera de su cuenta
 * para siempre. Es lo mas comun que necesita alguien de verdad y no existia.
 *
 * <h2>Por que el token se guarda en hash</h2>
 *
 * <p>La llave de baja de los avisos se guarda tal cual, y esta no. La diferencia
 * es lo que se puede hacer con cada una: con la de baja, quien lea la base deja
 * a alguien sin correos; con esta, entra en su cuenta. Mientras vive es una
 * credencial en toda regla —equivale a la contraseña—, asi que en la base va su
 * huella y el valor de verdad solo existe dentro del correo.
 *
 * <h2>Por que dura una hora y se usa una vez</h2>
 *
 * <p>Un enlace de recuperacion se queda en la bandeja de entrada para siempre.
 * Si no caduca, cualquiera que acceda a ese correo dentro de un año entra en la
 * cuenta; si no se consume, el mismo enlace sirve dos veces y basta con que
 * alguien lo reenvie sin darse cuenta.
 *
 * <h2>Lo que esto NO hace</h2>
 *
 * <p>No dice si un correo esta registrado. Quien pide recuperar recibe siempre
 * la misma respuesta exista o no la cuenta: si contestara distinto, este
 * formulario seria un comprobador de quien esta dado de alta, abierto y sin
 * sesion.
 */
@Service
public class Restablecimientos {

    /** Lo bastante para ir a por el correo, no tanto como para olvidarlo abierto. */
    static final Duration VALIDEZ = Duration.ofHours(1);

    private static final int BYTES = 32;
    private static final SecureRandom AZAR = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final PasswordEncoder cifrador;
    private final Clock reloj;

    public Restablecimientos(UsuarioRepository usuarios, PasswordEncoder cifrador, Clock reloj) {
        this.usuarios = usuarios;
        this.cifrador = cifrador;
        this.reloj = reloj;
    }

    /**
     * Abre un restablecimiento para ese correo, si existe la cuenta.
     *
     * @return el token que hay que mandar por correo, o vacio si no hay cuenta.
     *         Quien llama devuelve la misma respuesta en los dos casos.
     */
    @Transactional
    public Optional<String> abrirPara(String email) {
        return usuarios.findByEmail(email).map(usuario -> {
            String token = nuevoToken();

            usuario.setTokenReset(huellaDe(token));
            usuario.setTokenResetExpira(LocalDateTime.now(reloj).plus(VALIDEZ));
            usuarios.save(usuario);

            return token;
        });
    }

    /**
     * Pone la contraseña nueva si el token vale, y echa a las sesiones abiertas.
     *
     * <p>Lo segundo importa tanto como lo primero: si alguien te habia robado la
     * sesion, cambiar la contraseña sin invalidar los tokens no lo echa. Se
     * quedaria dentro veinticuatro horas mas, que es justo lo que la persona
     * cree haber evitado al restablecerla.
     *
     * @return false si el token no existe o ya ha caducado
     */
    @Transactional
    public boolean consumir(String token, String contrasenaNueva) {
        if (token == null || token.isBlank()) return false;

        // La regla, antes de tocar nada: si no vale, el token se queda vivo para
        // que la persona lo reintente en vez de quedarse sin enlace y sin cuenta.
        Contrasenas.exigirQueValga(contrasenaNueva);

        Optional<Usuario> encontrado = usuarios.findByTokenReset(huellaDe(token));
        if (encontrado.isEmpty()) return false;

        Usuario usuario = encontrado.get();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        if (usuario.getTokenResetExpira() == null || usuario.getTokenResetExpira().isBefore(ahora)) {
            return false;
        }

        usuario.setPassword(cifrador.encode(contrasenaNueva));
        // De un solo uso: un enlace que sirve dos veces basta con que alguien lo
        // reenvie sin darse cuenta.
        usuario.setTokenReset(null);
        usuario.setTokenResetExpira(null);
        usuario.setSesionesValidasDesde(alSegundo(ahora));
        usuarios.save(usuario);

        return true;
    }

    /**
     * Cambia la contraseña de quien ya esta dentro, comprobando la actual.
     *
     * <p>Se pide la actual aunque haya sesion: una sesion abierta en un ordenador
     * prestado no deberia bastar para quedarse con la cuenta.
     *
     * @return false si la actual no es correcta
     */
    @Transactional
    public boolean cambiar(String email, String actual, String nueva) {
        Contrasenas.exigirQueValga(nueva);

        Optional<Usuario> encontrado = usuarios.findByEmail(email);
        if (encontrado.isEmpty()) return false;

        Usuario usuario = encontrado.get();
        if (!cifrador.matches(actual, usuario.getPassword())) return false;

        usuario.setPassword(cifrador.encode(nueva));
        usuario.setSesionesValidasDesde(alSegundo(LocalDateTime.now(reloj)));
        usuarios.save(usuario);

        return true;
    }

    /**
     * Al segundo, porque el "emitido en" de un JWT solo tiene esa precision.
     *
     * <p>Sin truncar, quien cambia su contraseña y vuelve a entrar dentro del
     * mismo segundo se encuentra con que su token recien emitido ya esta
     * revocado: el iat baja a 10:00:01 y la marca vale 10:00:01,050. Es un fallo
     * intermitente de hasta un segundo, o sea de los que aparecen una vez de
     * cada cien y no hay quien los reproduzca.
     *
     * <p><b>El precio:</b> una sesion abierta en ese mismo segundo sobrevive al
     * restablecimiento, porque la comparacion es estricta. Se acepta a
     * conciencia: para aprovecharlo habria que iniciar sesion dentro del mismo
     * segundo en que la victima restablece, y quien ya tiene una sesion robada
     * no necesita iniciar ninguna. El caso real —la sesion de hace horas— se
     * revoca sin margen.
     */
    private static LocalDateTime alSegundo(LocalDateTime momento) {
        return momento.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    }

    private static String nuevoToken() {
        byte[] bytes = new byte[BYTES];
        AZAR.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 y no BCrypt.
     *
     * <p>BCrypt esta hecho para ser lento y frenar la fuerza bruta contra
     * contraseñas que la gente elige, que son adivinables. Esto son 32 bytes de
     * azar: no hay nada que adivinar, y ademas hay que poder buscarlo por
     * igualdad en la base, cosa que con BCrypt —que sala cada vez— es imposible.
     */
    static String huellaDe(String token) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 tiene que estar disponible", e);
        }
    }
}
