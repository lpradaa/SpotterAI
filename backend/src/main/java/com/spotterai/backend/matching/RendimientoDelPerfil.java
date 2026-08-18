package com.spotterai.backend.matching;

import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.textos.Mensaje;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Qué le falta a un perfil, medido en puntos de compatibilidad perdidos.
 *
 * <p>La diferencia con un "perfil completo al 60 %" es que aquí el número
 * significa algo: sin gimnasio no es que falte un campo, es que hay 15 de los
 * 100 puntos que nunca se pueden ganar con nadie. Eso se puede decidir; un
 * porcentaje de relleno, no.
 *
 * <p>Los pesos salen de {@link CalculadoraCompatibilidad}, no de una copia.
 *
 * @param puntosEnJuego cuantos puntos quedan fuera por los datos que faltan
 * @param huecos        lo que falta, de mas caro a mas barato
 */
public record RendimientoDelPerfil(int puntosEnJuego, List<HuecoDelPerfil> huecos) {

    /**
     * Por debajo de este ritmo se avisa.
     *
     * <p>No es el listón para ser constante, es el listón para que merezca la
     * pena decirlo: avisar a quien ya entrena dos veces por semana de que podría
     * entrenar tres es ruido.
     */
    private static final double UMBRAL_CONSTANCIA = 0.6;

    public boolean estaCompleto() {
        return huecos.isEmpty();
    }

    /** El que más cuesta, para poder decir una sola cosa en vez de una lista. */
    public HuecoDelPerfil masCaro() {
        return huecos.isEmpty() ? null : huecos.get(0);
    }

    /** Sin levantamientos ni historial, para quien llame sin esos datos. */
    public static RendimientoDelPerfil de(Usuario usuario, boolean tieneHorarios) {
        return de(usuario, tieneHorarios, false, Constancia.DESCONOCIDA);
    }

    public static RendimientoDelPerfil de(Usuario usuario, boolean tieneHorarios,
                                          boolean tieneLevantamientos) {
        return de(usuario, tieneHorarios, tieneLevantamientos, Constancia.DESCONOCIDA);
    }

    /**
     * Que hueco deja de tener sentido llamar "hueco".
     *
     * <p>La constancia no es un campo que falte: es algo que estas haciendo, y
     * te cuesta puntos con todo el mundo a la vez. Justo lo que este panel dice
     * medir —"puntos que no puedes ganar con nadie"— y por eso entra aqui aunque
     * no se rellene con un formulario.
     *
     * <p>Hacia falta ademas por una razon practica: sin esto, entrenar poco baja
     * tu puntuacion con todos los candidatos por igual y en la pantalla no
     * aparece ni una palabra sobre por que. Un numero que baja sin explicacion es
     * exactamente lo que esta aplicacion lleva evitando desde el principio.
     */
    public static RendimientoDelPerfil de(Usuario usuario, boolean tieneHorarios,
                                          boolean tieneLevantamientos,
                                          Constancia miConstancia) {
        List<HuecoDelPerfil> huecos = new ArrayList<>();

        if (!miConstancia.tieneHistorial()) {
            huecos.add(new HuecoDelPerfil("entrenamientos",
                    Mensaje.de("hueco.entrenamientos.nombre"),
                    (int) CalculadoraCompatibilidad.PESO_CONSTANCIA,
                    Mensaje.de("hueco.entrenamientos.motivo")));
        } else if (miConstancia.ritmo() < UMBRAL_CONSTANCIA) {
            int perdidos = (int) Math.round(
                    (1 - miConstancia.ritmo()) * CalculadoraCompatibilidad.PESO_CONSTANCIA);
            // El recuento va como mensaje dentro del mensaje, no formateado
            // aparte: compuesto a mano, la frase se traduce y el "3
            // entrenamientos" de dentro se queda en español.
            int recientes = miConstancia.entrenosRecientes();
            Mensaje cuantos = recientes == 1
                    ? Mensaje.de("hueco.constancia.uno")
                    : Mensaje.de("hueco.constancia.varios", recientes);

            huecos.add(new HuecoDelPerfil("constancia",
                    Mensaje.de("hueco.constancia.nombre"), perdidos,
                    Mensaje.de("hueco.constancia.motivo", cuantos)));
        }

        if (!tieneLevantamientos) {
            huecos.add(new HuecoDelPerfil("levantamientos",
                    Mensaje.de("hueco.levantamientos.nombre"),
                    (int) CalculadoraCompatibilidad.PESO_FUERZA,
                    Mensaje.de("hueco.levantamientos.motivo")));
        }
        if (!tieneHorarios) {
            huecos.add(new HuecoDelPerfil("horarios",
                    Mensaje.de("hueco.horarios.nombre"),
                    (int) CalculadoraCompatibilidad.PESO_HORARIO,
                    Mensaje.de("hueco.horarios.motivo")));
        }
        if (enBlanco(usuario.getNivel())) {
            huecos.add(new HuecoDelPerfil("nivel",
                    Mensaje.de("hueco.nivel.nombre"),
                    (int) CalculadoraCompatibilidad.PESO_NIVEL,
                    Mensaje.de("hueco.nivel.motivo")));
        }
        if (enBlanco(usuario.getObjetivos())) {
            huecos.add(new HuecoDelPerfil("objetivos",
                    Mensaje.de("hueco.objetivos.nombre"),
                    (int) CalculadoraCompatibilidad.PESO_OBJETIVO,
                    Mensaje.de("hueco.objetivos.motivo")));
        }
        if (usuario.getGimnasio() == null) {
            huecos.add(new HuecoDelPerfil("gimnasioId",
                    Mensaje.de("hueco.gimnasioId.nombre"),
                    (int) CalculadoraCompatibilidad.PESO_GIMNASIO,
                    Mensaje.de("hueco.gimnasioId.motivo")));
        }
        if (Rutina.desde(usuario.getRutina()).isEmpty()) {
            huecos.add(new HuecoDelPerfil("rutina",
                    Mensaje.de("hueco.rutina.nombre"),
                    (int) CalculadoraCompatibilidad.PESO_RUTINA,
                    Mensaje.de("hueco.rutina.motivo")));
        }
        if (usuario.getEdad() == null) {
            huecos.add(new HuecoDelPerfil("edad",
                    Mensaje.de("hueco.edad.nombre"),
                    (int) CalculadoraCompatibilidad.PESO_EDAD,
                    Mensaje.de("hueco.edad.motivo")));
        }

        huecos.sort(Comparator.comparingInt(HuecoDelPerfil::puntos).reversed());
        int enJuego = huecos.stream().mapToInt(HuecoDelPerfil::puntos).sum();

        return new RendimientoDelPerfil(enJuego, huecos);
    }

    private static boolean enBlanco(String valor) {
        return valor == null || valor.isBlank();
    }
}
