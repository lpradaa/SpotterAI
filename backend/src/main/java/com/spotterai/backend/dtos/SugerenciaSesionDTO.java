package com.spotterai.backend.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Lo que el formulario de propuesta trae ya puesto.
 *
 * <p>Sale del solape que el motor ya ha calculado, asi que abrir el formulario
 * es leer una frase ("el lunes 3 de 18:00 a 20:00") en vez de ponerse a cuadrar
 * horarios. Es una sugerencia, no una imposicion: los tres campos se pueden
 * cambiar antes de enviar.
 *
 * @param hayFranjas false cuando no hay ningun tramo comun, que es cuando la
 *                   interfaz tiene que decirlo en vez de sugerir una hora al azar
 * @param gimnasioNombre donde se da por hecho que quedais, cuando es el mismo
 *                       para los dos. Null si no lo es, y entonces hay {@code donde}
 * @param donde       los sitios entre los que elegir cuando cada uno entrena en
 *                    uno. Vacia cuando comparten gimnasio: no hay nada que preguntar
 */
public record SugerenciaSesionDTO(
        boolean hayFranjas,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean ambosFijos,
        String gimnasioNombre,
        List<OpcionDeGimnasio> donde
) {
    public SugerenciaSesionDTO {
        donde = donde == null ? List.of() : List.copyOf(donde);
    }

    /**
     * Un sitio posible para quedar.
     *
     * <p>Lleva de quien es porque en el formulario la pregunta no es "¿que
     * gimnasio?" sino "¿en el tuyo o en el suyo?", y sin decirlo son dos nombres
     * propios entre los que no hay forma de elegir.
     *
     * @param dequien "mio" o "suyo"
     */
    public record OpcionDeGimnasio(Long id, String nombre, String dequien) {}

    /** Cuando no hay nada en comun: que el formulario lo diga y no invente. */
    public static SugerenciaSesionDTO sinFranjas(String gimnasioNombre,
                                                 List<OpcionDeGimnasio> donde) {
        return new SugerenciaSesionDTO(false, null, null, null, false, gimnasioNombre, donde);
    }
}
