package com.spotterai.backend.controllers;

import com.spotterai.backend.textos.ErrorDeNegocio;
import com.spotterai.backend.textos.ErrorDePermiso;
import com.spotterai.backend.textos.Textos;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * El unico sitio donde un error se convierte en una frase.
 *
 * <p>Los controladores tenian este mismo bloque copiado veinte veces:
 *
 * <pre>
 * } catch (IllegalArgumentException e) {
 *     return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
 * }
 * </pre>
 *
 * <p>Y {@code getMessage()} devolvia la frase en español que llevaba dentro la
 * excepcion, que el frontend pinta tal cual. Aqui se redacta con el idioma de la
 * peticion, igual que el resto de lo que escribe el backend.
 *
 * <p>La forma de la respuesta no cambia —{@code {"error": "..."}}— porque hay
 * diez sitios del frontend leyendo ese campo.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    private final Textos textos;

    public ManejadorDeErrores(Textos textos) {
        this.textos = textos;
    }

    /** Lo que se le puede decir a alguien, ya con clave. */
    @ExceptionHandler(ErrorDeNegocio.class)
    public ResponseEntity<Map<String, String>> deNegocio(ErrorDeNegocio e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", textos.de(e.mensaje())));
    }

    /**
     * Lo que todavia lleva la frase dentro.
     *
     * <p>Va quedando menos, pero mientras quede se devuelve como antes: sin este
     * caso, migrar las excepciones una a una habria cambiado el codigo de estado
     * de las que faltaran.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> sinTraducir(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", String.valueOf(e.getMessage())));
    }

    /**
     * Mirar lo que no es tuyo, ya con clave.
     *
     * <p>403 y no 400: es lo que ya devolvian los controladores que distinguian
     * este caso, y el frontend lo trata distinto. Por eso hay dos tipos y no uno
     * con un campo — ver {@link ErrorDePermiso}.
     */
    @ExceptionHandler(ErrorDePermiso.class)
    public ResponseEntity<Map<String, String>> dePermiso(ErrorDePermiso e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", textos.de(e.mensaje())));
    }

    /**
     * Un 403 que todavia lleve la frase dentro.
     *
     * <p>Ya no queda ninguno, pero el caso se queda: sin el, una
     * {@code SecurityException} suelta —de Java, de una libreria— se convertiria
     * en un 500 en vez de en el 403 que el frontend sabe tratar.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> deSeguridad(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", String.valueOf(e.getMessage())));
    }
}
