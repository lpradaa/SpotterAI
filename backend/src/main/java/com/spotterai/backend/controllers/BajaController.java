package com.spotterai.backend.controllers;

import com.spotterai.backend.avisos.Bajas;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Dejar de recibir avisos por correo, desde el propio correo.
 *
 * <h2>Por que esto no pide iniciar sesion</h2>
 *
 * <p>Es lo unico de toda la API que se puede llamar sin estar identificado, y no
 * es un descuido. Quien quiere dejar de recibir correos no va a iniciar sesion
 * para conseguirlo; lo que hace es marcar el remitente como no deseado, y a
 * partir de ahi tampoco le llegan los avisos que si queria. Una salida que
 * cuesta trabajo no es una salida.
 *
 * <p>Lo que se arriesga con eso es acotado: con la llave de alguien, lo unico
 * que se puede hacer es cambiar si esa persona recibe correos. No identifica, no
 * abre sesion, no lee ni escribe nada mas. Y como es aleatoria de 32 bytes, no
 * se llega a ella probando.
 *
 * <h2>Por que POST y no un enlace que da de baja al abrirlo</h2>
 *
 * <p>Porque los antivirus de correo y los previsualizadores de las bandejas
 * siguen los enlaces por su cuenta para comprobar que no son peligrosos. Con una
 * baja de un solo clic por GET, media lista se daria de baja sola sin que nadie
 * hubiera tocado nada, y encima seria imposible de diagnosticar: en el registro
 * se veria gente dandose de baja normalmente.
 *
 * <p>Asi que el enlace del correo lleva a una pantalla con un boton, y el boton
 * es el que llama aqui.
 */
@RestController
@RequestMapping("/api/avisos")
public class BajaController {

    private final Bajas bajas;

    public BajaController(Bajas bajas) {
        this.bajas = bajas;
    }

    @PostMapping("/baja")
    public ResponseEntity<?> darDeBaja(@RequestBody Map<String, String> cuerpo) {
        return aplicar(cuerpo.get("token"), false);
    }

    /**
     * Volver a recibirlos, por si la baja fue un error.
     *
     * <p>La misma pantalla ofrece las dos cosas: una baja de la que no se puede
     * volver sin buscar donde deshacerla convierte un clic mal dado en un
     * problema.
     */
    @PostMapping("/alta")
    public ResponseEntity<?> darDeAlta(@RequestBody Map<String, String> cuerpo) {
        return aplicar(cuerpo.get("token"), true);
    }

    private ResponseEntity<?> aplicar(String token, boolean quiereRecibir) {
        if (bajas.cambiar(token, quiereRecibir)) {
            return ResponseEntity.ok(Map.of("avisosPorCorreo", quiereRecibir));
        }
        // 404 y no 403: no hay nada que decir sobre una llave que no existe, y
        // distinguir "no existe" de "no vale" solo ayudaria a quien pruebe.
        return ResponseEntity.status(404).body(Map.of("error", "Ese enlace ya no vale."));
    }
}
