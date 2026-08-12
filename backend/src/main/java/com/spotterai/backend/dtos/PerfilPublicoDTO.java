package com.spotterai.backend.dtos;

import com.spotterai.backend.matching.FranjaComun;
import com.spotterai.backend.matching.ProximaOcasion;

import java.util.List;

/**
 * Todo lo que se ve de otra persona al abrir su perfil.
 *
 * <p>Antes no habia forma de mirar a alguien: solo filas con un porcentaje y un
 * boton. En cualquier red social puedes ver a una persona antes de escribirle, y
 * esa es probablemente la diferencia mas grande entre una herramienta y un sitio
 * donde hay gente.
 *
 * <p>Va todo en una peticion: la ficha se abre entera o no se abre.
 *
 * @param entrenosUltimaSemana señal de que la persona esta activa, no una nota
 * @param hitos                lo que ha escrito, de mas reciente a mas antiguo
 * @param franjasEnComun       cuando coincidis, para dibujar la semana
 * @param yaConectado          si ya sois compañeros
 * @param solicitudPendiente   si hay una solicitud en marcha entre los dos
 */
public record PerfilPublicoDTO(
        Long id,
        String nombre,
        String avatar,
        String fotoUrl,
        String biografia,
        String nivel,
        String objetivos,
        /** Nombre legible de su rutina, o null si no la ha dicho. */
        String rutina,
        Integer edad,
        String gimnasioNombre,
        int compatibilidad,
        String etiquetaCompatibilidad,
        String resumenCompatibilidad,
        List<FranjaComun> franjasEnComun,
        List<HitoDTO> hitos,
        /** Sus marcas principales, para poder ver de donde sale la nota de fuerza. */
        List<LevantamientoDTO> levantamientos,
        /**
         * Las tuyas, para poder comparar sin salir de aqui.
         *
         * <p>La pantalla enseñaba sus marcas sueltas, y una lista de kilos ajenos
         * no dice nada: lo que decide si podeis cubriros es la diferencia con los
         * tuyos. Vacia en tu propio perfil, donde no hay nada que comparar.
         */
        List<LevantamientoDTO> misLevantamientos,
        long entrenosUltimaSemana,
        /**
         * Veces que habeis quedado y el dia ya ha pasado.
         *
         * No dice "habeis entrenado juntos", que seria inventar: consta que lo
         * acordasteis y consta que el dia llego, y eso es todo lo que sabemos.
         */
        long sesionesJuntos,
        boolean yaConectado,
        boolean solicitudPendiente,
        /**
         * Si es tu propio perfil.
         *
         * Hace falta desde que el perfil es una pagina con URL propia: /yo lleva
         * aqui, y la compatibilidad de alguien consigo mismo no significa nada,
         * asi que ese bloque no se calcula ni se pinta.
         */
        boolean esMio,

        /**
         * El primer dia concreto en el que podriais entrenar juntos, o null.
         *
         * <p>Es el calculo mas accionable del proyecto y estaba encerrado detras
         * de ser compañeros: solo lo pedia el formulario de proponer sesion, que
         * exige solicitud aceptada. O sea que en la ficha de alguien con quien
         * todavia no has conectado —justo donde se decide si conectar— se veia
         * una rejilla de solape pero nunca la frase que la convierte en un plan.
         *
         * <p>Es solo lectura: baja la regla semanal a un dia del calendario, no
         * crea ni reserva nada. Null si no hay ninguna franja en comun, y
         * entonces la pantalla se calla en vez de inventar una fecha.
         */
        ProximaOcasion proximaOcasion
) {}
