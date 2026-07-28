package com.spotterai.backend.matching;

import com.spotterai.backend.models.Usuario;

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
            huecos.add(new HuecoDelPerfil("entrenamientos", "Entrenamientos",
                    (int) CalculadoraCompatibilidad.PESO_CONSTANCIA,
                    "Sin ningún entrenamiento registrado no hay forma de saber si apareces, "
                            + "que es lo que más mira quien busca compañero."));
        } else if (miConstancia.ritmo() < UMBRAL_CONSTANCIA) {
            int perdidos = (int) Math.round(
                    (1 - miConstancia.ritmo()) * CalculadoraCompatibilidad.PESO_CONSTANCIA);
            huecos.add(new HuecoDelPerfil("constancia", "Constancia", perdidos,
                    "Has registrado %d %s en el último mes. Una pareja entrena tan a menudo como el que menos aparece, así que esto te resta con todo el mundo."
                            .formatted(miConstancia.entrenosRecientes(),
                                    miConstancia.entrenosRecientes() == 1 ? "entrenamiento" : "entrenamientos")));
        }

        if (!tieneLevantamientos) {
            huecos.add(new HuecoDelPerfil("levantamientos", "Levantamientos",
                    (int) CalculadoraCompatibilidad.PESO_FUERZA,
                    "Sin saber cuánto mueves, nadie puede saber si podríais cubriros con la barra."));
        }
        if (!tieneHorarios) {
            huecos.add(new HuecoDelPerfil("horarios", "Horario",
                    (int) CalculadoraCompatibilidad.PESO_HORARIO,
                    "Sin saber cuándo entrenas no hay forma de cruzarte con nadie."));
        }
        if (enBlanco(usuario.getNivel())) {
            huecos.add(new HuecoDelPerfil("nivel", "Nivel",
                    (int) CalculadoraCompatibilidad.PESO_NIVEL,
                    "Entrenar con alguien de nivel muy distinto rara vez funciona."));
        }
        if (enBlanco(usuario.getObjetivos())) {
            huecos.add(new HuecoDelPerfil("objetivos", "Objetivo",
                    (int) CalculadoraCompatibilidad.PESO_OBJETIVO,
                    "Buscar lo mismo es lo que hace que las sesiones cuadren."));
        }
        if (usuario.getGimnasio() == null) {
            huecos.add(new HuecoDelPerfil("gimnasioId", "Gimnasio",
                    (int) CalculadoraCompatibilidad.PESO_GIMNASIO,
                    "Coincidir en horario no sirve de nada en dos gimnasios distintos."));
        }
        if (Rutina.desde(usuario.getRutina()).isEmpty()) {
            huecos.add(new HuecoDelPerfil("rutina", "Rutina",
                    (int) CalculadoraCompatibilidad.PESO_RUTINA,
                    "Sin saber cómo repartes la semana, no se puede saber si haríais lo mismo el mismo día."));
        }
        if (usuario.getEdad() == null) {
            huecos.add(new HuecoDelPerfil("edad", "Edad",
                    (int) CalculadoraCompatibilidad.PESO_EDAD,
                    "Pesa poco, pero desempata."));
        }

        huecos.sort(Comparator.comparingInt(HuecoDelPerfil::puntos).reversed());
        int enJuego = huecos.stream().mapToInt(HuecoDelPerfil::puntos).sum();

        return new RendimientoDelPerfil(enJuego, huecos);
    }

    private static boolean enBlanco(String valor) {
        return valor == null || valor.isBlank();
    }
}
