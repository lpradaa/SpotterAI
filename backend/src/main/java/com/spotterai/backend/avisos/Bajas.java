package com.spotterai.backend.avisos;

import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Dejar de recibir avisos, y volver a recibirlos.
 *
 * <h2>Por que no basta con una casilla en el perfil</h2>
 *
 * <p>Quien quiere dejar de recibir correos no va a iniciar sesion para
 * conseguirlo. Obligarle a ello es la forma educada de no dejarle salir, y lo
 * que consigue de verdad es que marque el correo como no deseado, que es peor
 * para todos: a partir de ahi no llegan tampoco los que si queria.
 *
 * <p>Asi que la baja se hace desde un enlace del propio correo, sin sesion. La
 * casilla del perfil existe tambien, para quien ya esta dentro.
 *
 * <h2>La llave</h2>
 *
 * <p>Aleatoria y no derivada del id: si fuera un id cifrado o un numero, se
 * podria dar de baja a cualquiera probando. Y no sirve para nada mas —no
 * autentica, no abre sesion, no deja leer nada— porque va en el cuerpo de un
 * correo, que es el sitio menos privado que hay.
 *
 * <p>Se crea la primera vez que se le manda un aviso a alguien, que es el primer
 * momento en que el enlace tiene que existir. Antes de eso no hace falta y no
 * tiene sentido tenerla.
 */
@Service
public class Bajas {

    /**
     * 32 bytes. No es una contraseña que nadie escriba a mano, asi que no hay
     * razon para acortarla, y de fuerza bruta no se saca nada.
     */
    private static final int BYTES = 32;

    private static final SecureRandom AZAR = new SecureRandom();

    private final UsuarioRepository usuarios;

    public Bajas(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /**
     * La llave de esta persona, creandola si todavia no tenia.
     *
     * <p>Se conserva entre correos a proposito. Si se generase una nueva cada
     * vez, el enlace de un correo de hace dos semanas dejaria de funcionar, y
     * ese es justo el correo que alguien abre cuando se harta.
     */
    @Transactional
    public String llaveDe(Usuario usuario) {
        if (usuario.getTokenAvisos() == null) {
            usuario.setTokenAvisos(nueva());
            usuarios.save(usuario);
        }
        return usuario.getTokenAvisos();
    }

    /**
     * Cambia la preferencia de quien tenga esa llave.
     *
     * @return true si la llave era buena y se ha aplicado
     */
    @Transactional
    public boolean cambiar(String llave, boolean quiereRecibir) {
        if (llave == null || llave.isBlank()) return false;

        return usuarios.findByTokenAvisos(llave)
                .map(usuario -> {
                    usuario.setAvisosPorCorreo(quiereRecibir);
                    usuarios.save(usuario);
                    return true;
                })
                .orElse(false);
    }

    private static String nueva() {
        byte[] bytes = new byte[BYTES];
        AZAR.nextBytes(bytes);
        // Sin relleno y en variante URL: esto viaja dentro de un enlace.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
