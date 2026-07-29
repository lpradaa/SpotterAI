package com.spotterai.backend.config;

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

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        GalletaDeSesion.leerDe(request)
                .filter(jwtUtil::validarToken)
                .ifPresent(token -> {
                    String email = jwtUtil.extraerEmail(token);

                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(email, null, new ArrayList<>()));
                });

        filterChain.doFilter(request, response);
    }
}
