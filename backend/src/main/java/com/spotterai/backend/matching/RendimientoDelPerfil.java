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

    public boolean estaCompleto() {
        return huecos.isEmpty();
    }

    /** El que más cuesta, para poder decir una sola cosa en vez de una lista. */
    public HuecoDelPerfil masCaro() {
        return huecos.isEmpty() ? null : huecos.get(0);
    }

    /** Sin levantamientos, para quien llame sin ese dato. */
    public static RendimientoDelPerfil de(Usuario usuario, boolean tieneHorarios) {
        return de(usuario, tieneHorarios, false);
    }

    public static RendimientoDelPerfil de(Usuario usuario, boolean tieneHorarios,
                                          boolean tieneLevantamientos) {
        List<HuecoDelPerfil> huecos = new ArrayList<>();

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
