package com.spotterai.backend.seguridad;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freno a la fuerza bruta contra el inicio de sesión.
 *
 * <p>El repositorio es público y el endpoint de login no tenía ningún límite:
 * se podían probar contraseñas a la velocidad que aguantara la red. BCrypt
 * encarece cada intento, pero encarecerlos no es lo mismo que impedirlos, y
 * contra una contraseña corta la diferencia no basta.
 *
 * <p><b>Dos llaves, con topes distintos y a propósito.</b> Por correo, porque es
 * lo que de verdad protege una cuenta concreta y es un dato que el atacante no
 * puede falsear: si quiere entrar en la tuya, tiene que nombrarla. Y por
 * dirección, para frenar el barrido de muchas cuentas desde un solo sitio. El
 * tope por dirección es mucho más alto porque detrás de una sola IP puede haber
 * un gimnasio entero: apretarlo dejaría fuera a gente que no ha hecho nada.
 *
 * <p><b>Cuentan también los correos que no existen.</b> Si solo se contaran los
 * fallos de cuentas reales, ver cuál se bloquea y cuál no diría exactamente
 * quién está registrado, y el freno se convertiría en un listador de usuarios.
 *
 * <p>Vive en memoria, que es la decisión correcta para el tamaño de esto: en un
 * despliegue con varias instancias haría falta algo compartido —Redis o la
 * propia base— porque cada instancia contaría por su cuenta. Está anotado
 * aquí y no escondido para que quien lo despliegue lo sepa.
 */
@Component
public class ControlDeIntentos {

    /**
     * Intentos fallidos contra un mismo correo antes de cerrar la puerta.
     *
     * Publica porque las pruebas del controlador la necesitan para agotar el
     * cupo, y escribir el 5 a mano alli seria una copia que se queda vieja en
     * cuanto alguien afloje el limite.
     */
    public static final int MAX_POR_CORREO = 5;

    /**
     * Intentos fallidos desde una misma dirección.
     *
     * Muy por encima del de correo: detrás de una IP puede haber un gimnasio
     * entero compartiendo salida, y equivocarse de contraseña es normal.
     */
    static final int MAX_POR_DIRECCION = 30;

    /** Sin fallos nuevos durante este rato, la cuenta vuelve a cero. */
    static final Duration VENTANA = Duration.ofMinutes(15);

    private final Clock reloj;

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    public ControlDeIntentos(Clock reloj) {
        this.reloj = reloj;
    }

    /** Fallos acumulados y cuándo empezó a contarse. */
    private static final class Registro {
        int fallos;
        Instant desde;

        Registro(Instant desde) {
            this.desde = desde;
            this.fallos = 0;
        }
    }

    public static String claveDeCorreo(String correo) {
        return "correo:" + (correo == null ? "" : correo.trim().toLowerCase());
    }

    public static String claveDeDireccion(String direccion) {
        return "ip:" + (direccion == null ? "desconocida" : direccion);
    }

    /** Si esta llave ha agotado su cupo y hay que negarle el paso. */
    public boolean bloqueada(String clave) {
        Registro registro = registros.get(clave);
        if (registro == null) return false;

        synchronized (registro) {
            if (caducado(registro)) return false;
            return registro.fallos >= topeDe(clave);
        }
    }

    /** Lo que queda para que se libere, para poder decírselo a quien llama. */
    public Duration esperaDe(String clave) {
        Registro registro = registros.get(clave);
        if (registro == null) return Duration.ZERO;

        synchronized (registro) {
            Duration transcurrido = Duration.between(registro.desde, ahora());
            Duration resto = VENTANA.minus(transcurrido);
            return resto.isNegative() ? Duration.ZERO : resto;
        }
    }

    /** Un intento que no ha colado. */
    public void fallo(String clave) {
        Registro registro = registros.computeIfAbsent(clave, k -> new Registro(ahora()));

        synchronized (registro) {
            if (caducado(registro)) {
                registro.fallos = 0;
                registro.desde = ahora();
            }
            registro.fallos++;
        }
    }

    /**
     * Un acceso correcto: se olvida lo anterior de esa llave.
     *
     * Solo la del correo. Perdonar también la de la dirección permitiría a
     * quien tenga una cuenta propia limpiar el contador cada pocos intentos y
     * seguir probando contra las demás desde el mismo sitio.
     */
    public void acierto(String clave) {
        registros.remove(clave);
    }

    /** Fallos vivos de una llave. Solo para pruebas y diagnóstico. */
    int fallosDe(String clave) {
        Registro registro = registros.get(clave);
        if (registro == null) return 0;
        synchronized (registro) {
            return caducado(registro) ? 0 : registro.fallos;
        }
    }

    /**
     * Tira lo que ya no cuenta.
     *
     * <p>Sin esto el mapa crece con cada correo distinto que alguien pruebe, lo
     * que convierte el propio freno en la forma más cómoda de tumbar el
     * servidor: bastaría con inventarse direcciones.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void limpiar() {
        registros.entrySet().removeIf(entrada -> {
            synchronized (entrada.getValue()) {
                return caducado(entrada.getValue());
            }
        });
    }

    private int topeDe(String clave) {
        return clave.startsWith("ip:") ? MAX_POR_DIRECCION : MAX_POR_CORREO;
    }

    private boolean caducado(Registro registro) {
        return Duration.between(registro.desde, ahora()).compareTo(VENTANA) >= 0;
    }

    private Instant ahora() {
        return reloj.instant();
    }
}
