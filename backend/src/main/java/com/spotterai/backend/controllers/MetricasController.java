package com.spotterai.backend.controllers;

import com.spotterai.backend.metricas.EmbudoDeCompatibilidad;
import com.spotterai.backend.metricas.ServicioEmbudo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Si el motor acierta.
 *
 * <p>Solo agregados por tramo, nunca quien pidio que a quien. Un embudo de tres
 * filas no dice nada de nadie; la lista de solicitudes que lo forma diria
 * bastante, y por eso no sale de aqui.
 *
 * <p>Va detras del login como todo lo demas. Podria argumentarse que es una
 * pantalla de desarrollo y deberia estar cerrada del todo, pero no hay roles en
 * esta aplicacion y no se va a inventar uno para esto: lo que devuelve es el
 * comportamiento agregado de la comunidad, que es de la comunidad.
 */
@RestController
@RequestMapping("/api/metricas")
public class MetricasController {

    private final ServicioEmbudo servicioEmbudo;

    public MetricasController(ServicioEmbudo servicioEmbudo) {
        this.servicioEmbudo = servicioEmbudo;
    }

    @GetMapping("/embudo")
    public ResponseEntity<EmbudoDeCompatibilidad> embudo() {
        return ResponseEntity.ok(servicioEmbudo.calcular());
    }
}
