package com.spotterai.backend.matching;

import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Levantamiento;
import com.spotterai.backend.models.Usuario;

import java.util.List;

/**
 * Todo lo que el motor necesita saber de una persona para puntuarla.
 *
 * <p>Existe porque {@code calcular} llego a tener seis argumentos posicionales
 * —dos usuarios, dos listas de horarios, dos de levantamientos— y añadir la
 * constancia lo dejaba en ocho, de los cuales seis del mismo tipo. Cruzar los
 * mios con los suyos en una llamada asi no da error de compilacion: da una
 * puntuacion equivocada, y encima simetrica, asi que ni siquiera se nota rara.
 *
 * <p>Con esto la llamada es {@code calcular(mio, suyo)} y no hay forma de
 * mezclar la mitad de uno con la mitad del otro.
 */
public record PerfilDeMatch(
        Usuario usuario,
        List<Disponibilidad> horarios,
        List<Levantamiento> levantamientos,
        Constancia constancia) {

    public PerfilDeMatch {
        horarios = horarios == null ? List.of() : horarios;
        levantamientos = levantamientos == null ? List.of() : levantamientos;
        constancia = constancia == null ? Constancia.DESCONOCIDA : constancia;
    }

    /** Solo con lo que se sabia antes de que existieran las marcas y la constancia. */
    public static PerfilDeMatch de(Usuario usuario, List<Disponibilidad> horarios) {
        return new PerfilDeMatch(usuario, horarios, List.of(), Constancia.DESCONOCIDA);
    }

    public static PerfilDeMatch de(Usuario usuario, List<Disponibilidad> horarios,
                                   List<Levantamiento> levantamientos) {
        return new PerfilDeMatch(usuario, horarios, levantamientos, Constancia.DESCONOCIDA);
    }
}
