package com.spotterai.backend.textos;

/**
 * Un texto sin redactar: la clave del catalogo y lo que va en sus huecos.
 *
 * <p>El motor de esta aplicacion escribe. "Los dos vais siempre 2 dias a la
 * misma hora (Lunes y Miercoles)", "Son 15 puntos que ahora mismo no puedes
 * ganar con nadie": esa prosa es una de las decisiones fuertes del proyecto,
 * porque es lo que convierte un porcentaje en un argumento. Y estaba escrita en
 * español dentro de las calculadoras.
 *
 * <p>La alternativa era que el backend dejara de escribir y devolviera datos
 * para que el frontend redactara. Se descarto por una razon que decide: el
 * backend manda correos, y esos hay que traducirlos en el servidor si o si.
 * Habrian acabado siendo dos sistemas de traduccion. Ademas, que la calculadora
 * se explique a si misma es justo lo que hace que la explicacion no pueda
 * contradecir al numero.
 *
 * <p>Asi que el motor sigue escribiendo, pero escribe claves. La frase se
 * compone en el borde —{@link Textos}— con el idioma de quien pregunta.
 *
 * @param clave la clave en messages_*.properties
 * @param args  lo que rellena los {0}, {1}… de esa clave. Puede llevar otros
 *              {@code Mensaje} dentro: un texto que se compone de textos —"un
 *              dia", "Lunes y Miercoles"— tambien cambia de idioma, y esa es la
 *              parte que se olvida cuando se traduce solo la frase de fuera.
 */
public record Mensaje(String clave, Object... args) {

    public static Mensaje de(String clave, Object... args) {
        return new Mensaje(clave, args);
    }
}
