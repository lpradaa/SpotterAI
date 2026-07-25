package com.spotterai.backend.config;

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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 1. Buscamos la cabecera de seguridad en la petición de Angular
        String authHeader = request.getHeader("Authorization");

        // 2. Los tokens estándar siempre empiezan por la palabra "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            
            // Recortamos la palabra "Bearer " para quedarnos solo con el código del token
            String token = authHeader.substring(7);

            // 3. Usamos la herramienta que creamos antes para ver si no está caducado ni manipulado
            if (jwtUtil.validarToken(token)) {
                
                // Extraemos quién es el dueño del token
                String email = jwtUtil.extraerEmail(token);

                // 4. ¡Vía libre! Le decimos a Spring Security que este usuario es de confianza
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(email, null, new ArrayList<>());
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 5. Dejamos que la petición continúe su camino hacia el Controlador
        filterChain.doFilter(request, response);
    }
}
