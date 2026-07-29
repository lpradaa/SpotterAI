package com.spotterai.backend.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Fuerza a que la galleta del CSRF exista antes de que haga falta.
 *
 * <h2>El fallo que arregla</h2>
 *
 * <p>Spring Security calcula el token de CSRF de forma perezosa: solo lo genera
 * —y por tanto solo manda la galleta— cuando alguien lo pide durante la
 * peticion. El login esta en la lista de exentos, asi que nadie lo pide ahi y la
 * respuesta del login sale <b>sin</b> galleta de CSRF.
 *
 * <p>Justo despues de entrar, el frontend abre el canal de eventos, que empieza
 * pidiendo un ticket por POST. Ese POST ya necesita la cabecera, pero no hay
 * galleta de la que copiarla: 403, el interceptor lo lee como sesion invalida y
 * te devuelve al login diciendo que ha caducado. Se entra y se sale solo.
 *
 * <p>Este filtro toca el token en cada peticion, que es lo unico que hace falta
 * para que la galleta se emita. Cuesta nada y quita una carrera que depende de
 * en que orden haga el frontend sus primeras llamadas, que es el peor tipo de
 * error: aparece o no segun la maquina y el dia.
 *
 * <p>Va de la mano de {@code CsrfTokenRequestAttributeHandler} en la
 * configuracion, que desactiva el enmascarado XOR del valor. Con el enmascarado
 * puesto, el valor de la galleta y el que espera el servidor no coinciden, y
 * copiar uno en el otro —que es lo que hace el interceptor— nunca valdria.
 */
public class FiltroGalletaCsrf extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {

        CsrfToken token = (CsrfToken) peticion.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            // Con esta llamada basta: es la que despierta al repositorio y hace
            // que la galleta salga en la respuesta.
            token.getToken();
        }

        cadena.doFilter(peticion, respuesta);
    }
}
