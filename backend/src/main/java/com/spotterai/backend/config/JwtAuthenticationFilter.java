package com.spotterai.backend.config;

import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.UsuarioRepository;
import com.spotterai.backend.seguridad.GalletaDeSesion;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;

/**
 * Identifica a quien llama a partir de la galleta de sesion.
 *
 * <p>Antes leia la cabecera {@code Authorization}, que obligaba al frontend a
 * tener el token a mano y por tanto guardado en un sitio que el JavaScript de la
 * pagina puede leer. Ahora viene en una galleta {@code HttpOnly} y el navegador
 * la manda solo (ver {@link com.spotterai.backend.seguridad.GalletaDeSesion}).
 *
 * <p>La cabecera ya no se acepta, a proposito: si se aceptara "por
 * compatibilidad", seguiria habiendo una forma de entrar que necesita el token
 * en JavaScript, y bastaria con que un solo sitio del frontend la usara para
 * que todo esto no hubiera servido de nada.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarios;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UsuarioRepository usuarios) {
        this.jwtUtil = jwtUtil;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        GalletaDeSesion.leerDe(request)
                .filter(jwtUtil::validarToken)
                .ifPresent(token -> {
                    String email = jwtUtil.extraerEmail(token);

                    if (revocado(email, token)) return;

                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(email, null, new ArrayList<>()));
                });

        filterChain.doFilter(request, response);
    }

    /**
     * Si este token es anterior al ultimo cambio de contraseña de esa persona.
     *
     * <p>Es lo que hace que cambiar la contraseña eche de verdad a quien te
     * hubiera robado la sesion. Sin esto, el token robado sigue valiendo
     * veinticuatro horas y el cambio solo tranquiliza.
     *
     * <p><b>El coste:</b> esto convierte la autenticacion en una consulta por
     * peticion, cuando antes no tocaba la base en absoluto. Es el precio de
     * poder revocar, y no hay forma de evitarlo con tokens firmados: revocar
     * exige estado en el servidor. A esta escala —una consulta indexada por
     * correo, junto a las varias que ya hace cada peticion— no se nota.
     *
     * <p>Si el usuario no existe, se deja pasar y que responda 404 quien
     * corresponda: aqui no se decide eso.
     */
    private boolean revocado(String email, String token) {
        return usuarios.findByEmail(email)
                .map(Usuario::getSesionesValidasDesde)
                .map(desde -> jwtUtil.emitidoEn(token)
                        .isBefore(desde.atZone(ZoneId.systemDefault()).toInstant()))
                .orElse(false);
    }
}
