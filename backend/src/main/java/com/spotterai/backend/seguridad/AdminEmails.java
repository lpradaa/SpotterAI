package com.spotterai.backend.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Quien puede ver los reportes, mientras no exista un sistema de roles.
 *
 * <h2>Por que una lista por variable de entorno y no una tabla con roles</h2>
 *
 * <p>Un sistema de roles de verdad —tabla, asignacion, quiza jerarquia— es
 * infraestructura para una organizacion con varios moderadores que cambian con
 * el tiempo. Hoy no hay ni un usuario real, y mucho menos un equipo de
 * moderacion; construir eso ahora seria construir para un problema que no
 * existe todavia, a costa de la pantalla que si hace falta: que el reporte se
 * pueda leer por alguien.
 *
 * <p>Una lista de correos en {@code ADMIN_EMAILS} es lo minimo que resuelve
 * "que alguien pueda ver esto" sin construir esa infraestructura por
 * adelantado. El dia que haga falta de verdad —mas de un moderador, roles
 * distintos, quitarle acceso a alguien sin redesplegar— se sustituye esta
 * clase por la de verdad sin que el resto de la aplicacion se entere: nadie
 * fuera de aqui sabe como se decide quien es admin.
 *
 * <h2>Por que no es una decision de base de datos</h2>
 *
 * <p>Justo lo contrario que las contraseñas o los tokens: aqui no hay nada que
 * proteger con un hash, es una lista de quien tiene permiso, no un secreto de
 * cada usuario. Una variable de entorno es exactamente el sitio para eso, y
 * cambiarla no pide una migracion.
 */
@Component
public class AdminEmails {

    private final List<String> correos;

    public AdminEmails(@Value("${spotterai.admin.correos:}") String correosSeparadosPorComas) {
        this.correos = Arrays.stream(correosSeparadosPorComas.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
    }

    public boolean esAdmin(String email) {
        return email != null && correos.contains(email.toLowerCase(Locale.ROOT));
    }
}
