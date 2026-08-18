package com.spotterai.backend.textos;

/**
 * Un "esto no se puede hacer" que se le va a enseñar a alguien.
 *
 * <p>Lleva la clave del texto y no el texto: lo que se lanza aqui acaba pintado
 * tal cual en la pantalla —los controladores devuelven {@code getMessage()} y el
 * frontend lo enseña— asi que con la frase escrita dentro salia en español en
 * una pantalla en ingles. Quien la redacta es {@link Textos}, con el idioma de
 * la peticion, y eso pasa en un solo sitio: {@code ManejadorDeErrores}.
 *
 * <p>No hereda de {@code IllegalArgumentException} por gusto: los controladores
 * ya la capturan en todas partes, asi que heredando de ella este cambio no
 * obliga a tocar ningun {@code catch} para seguir funcionando. Lo que cambia es
 * que ahora hay un sitio mejor donde capturarla.
 *
 * <p>Se distingue de un fallo de programacion —un indice fuera de rango, un nulo
 * inesperado— a proposito: aquello no se le enseña a nadie, esto si.
 */
public class ErrorDeNegocio extends IllegalArgumentException {

    private final transient Mensaje mensaje;

    public ErrorDeNegocio(Mensaje mensaje) {
        // El mensaje de Java se queda con la clave. No se le enseña a nadie:
        // es lo que sale en los registros, y ahi una clave localiza el sitio
        // mejor que una frase traducida.
        super(mensaje.clave());
        this.mensaje = mensaje;
    }

    /** Atajo para el caso corriente: una clave sin nada dentro. */
    public static ErrorDeNegocio de(String clave, Object... args) {
        return new ErrorDeNegocio(Mensaje.de(clave, args));
    }

    public Mensaje mensaje() {
        return mensaje;
    }
}
