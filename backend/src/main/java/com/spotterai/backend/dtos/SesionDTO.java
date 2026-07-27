package com.spotterai.backend.dtos;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Una sesion tal y como la ve una de las dos personas.
 *
 * <p>Los campos que empiezan por "puedo" no son datos de la sesion sino de quien
 * mira: el invitado puede aceptar y el proponente no, y despues de que pase la
 * hora los dos pueden apuntarla. Se calculan en el servidor porque ahi estan las
 * reglas; que el navegador las dedujera por su cuenta seria tener las mismas
 * reglas escritas dos veces, y de esas dos copias una acabaria mintiendo.
 *
 * @param conNombre    el otro, ya resuelto: quien mira nunca es "el otro"
 * @param mia          si la propuse yo
 * @param yaConfirmada si yo ya he dicho que fui
 */
public record SesionDTO(
        Long id,
        Long conId,
        String conNombre,
        String conAvatar,
        String conFotoUrl,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String gimnasioNombre,
        String estado,
        String nota,
        boolean mia,
        boolean puedoResponder,
        boolean puedoCancelar,
        boolean puedoConfirmar,
        boolean yaConfirmada
) {}
