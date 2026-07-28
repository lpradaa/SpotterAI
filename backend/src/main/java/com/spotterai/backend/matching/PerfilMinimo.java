package com.spotterai.backend.matching;

import com.spotterai.backend.models.Usuario;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo que hay que saber de alguien para poder emparejarlo.
 *
 * <p>La regla es <b>obligatorio lo que se declara, opcional lo que se mide</b>, y
 * la diferencia no es de comodidad.
 *
 * <p>Nivel, objetivo, gimnasio, edad y rutina son declaraciones: baratas, y sin
 * ningún motivo para mentir. Exigirlas cuesta cinco desplegables y a cambio hace
 * que todas las puntuaciones sean comparables entre sí, porque dejan de
 * calcularse unas con seis factores y otras con tres.
 *
 * <p>Los levantamientos, en cambio, son una medición. Obligar a ponerlos no
 * produce datos: produce números inventados por quien no los sabe, y entrarían
 * derechos al factor del que depende el nombre del producto. Un dato ausente al
 * menos sabe que lo está; uno inventado no, y el motor lo trata como un hecho.
 * Por eso siguen siendo opcionales, y por eso el panel de "esto te está costando
 * puntos" dice lo que valen en vez de bloquear el paso.
 *
 * <p>Lo mismo con la constancia, que ni siquiera se rellena: se gana yendo.
 *
 * <p>Cada campo exigido tiene una respuesta honesta para quien no encaje en
 * ninguna casilla —{@code Rutina.LIBRE} es literalmente eso, "sin rutina fija",
 * y es un dato, no un hueco—. Sin esa salida, exigir un campo solo consigue que
 * la gente marque la primera opción, y entonces el motor está más seguro de un
 * dato peor.
 *
 * @param queFalta claves de los campos que faltan, en el orden en que se piden
 */
public record PerfilMinimo(List<String> queFalta) {

    public boolean estaCompleto() {
        return queFalta.isEmpty();
    }

    public static PerfilMinimo de(Usuario usuario, boolean tieneHorarios) {
        List<String> falta = new ArrayList<>();

        // El horario primero: es el 40 % de la puntuacion y lo unico sin lo cual
        // no se puede cruzar a nadie con nadie.
        if (!tieneHorarios) falta.add("horarios");
        if (usuario.getGimnasio() == null) falta.add("gimnasioId");
        if (enBlanco(usuario.getNivel())) falta.add("nivel");
        if (enBlanco(usuario.getObjetivos())) falta.add("objetivos");
        if (Rutina.desde(usuario.getRutina()).isEmpty()) falta.add("rutina");
        if (usuario.getEdad() == null) falta.add("edad");

        return new PerfilMinimo(List.copyOf(falta));
    }

    private static boolean enBlanco(String valor) {
        return valor == null || valor.isBlank();
    }
}
