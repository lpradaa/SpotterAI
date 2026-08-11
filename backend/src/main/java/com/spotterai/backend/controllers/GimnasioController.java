package com.spotterai.backend.controllers;

import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.services.GimnasioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gimnasios")
public class GimnasioController {

    private final GimnasioService gimnasioService;

    // Inyectamos el servicio de Gimnasios
    public GimnasioController(GimnasioService gimnasioService) {
        this.gimnasioService = gimnasioService;
    }

    /**
     * 1. OBTENER TODOS LOS GIMNASIOS
     * GET http://localhost:8080/api/gimnasios
     * Uso: Para rellenar el <select> en el Frontend cuando el usuario edita su perfil.
     */
    @GetMapping
    public ResponseEntity<List<Gimnasio>> obtenerTodos() {
        List<Gimnasio> gimnasios = gimnasioService.obtenerTodos();
        return ResponseEntity.ok(gimnasios);
    }

    /**
     * 2. BUSCAR GIMNASIOS POR CIUDAD
     * GET http://localhost:8080/api/gimnasios/ciudad/{ciudad}
     * Uso: Por si en el futuro quieres añadir un buscador para que el usuario filtre su zona.
     */
    @GetMapping("/ciudad/{ciudad}")
    public ResponseEntity<List<Gimnasio>> buscarPorCiudad(@PathVariable String ciudad) {
        List<Gimnasio> gimnasios = gimnasioService.buscarPorCiudad(ciudad);
        if (gimnasios.isEmpty()) {
            return ResponseEntity.noContent().build(); // Devuelve 204 si no hay gimnasios en esa ciudad
        }
        return ResponseEntity.ok(gimnasios);
    }

    /*
     * Aqui habia un POST abierto a cualquiera con sesion, que guardaba el
     * Gimnasio del cuerpo tal cual. Era de cuando la base se poblaba a mano con
     * Postman; ahora la siembra la hace SembradorDemo y los gimnasios que faltan
     * los añade la gente desde su perfil, que pasa por buscarOCrear y compara
     * los nombres normalizados.
     *
     * Se quita porque dejarlo seria dejar abierta la puerta que el arreglo
     * cierra: sin normalizar, "McFit Centro", "mcfit centro" y "McFit  Centro"
     * son tres gimnasios, y dos personas del mismo edificio pasan a valer 0,25x
     * de solape de horario. Fragmentar el catalogo no rompe nada visible: solo
     * hace que el motor puntue peor sin que nadie sepa por que.
     */
}
