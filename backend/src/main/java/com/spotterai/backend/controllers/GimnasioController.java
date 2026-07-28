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

    /**
     * 3. AÑADIR UN GIMNASIO NUEVO
     * POST http://localhost:8080/api/gimnasios
     * Uso: Temporalmente lo usaremos nosotros para "poblar" la base de datos con Postman o Angular.
     * En un futuro comercial (B2B), esto lo usarían los dueños de los centros deportivos.
     */
    @PostMapping
    public ResponseEntity<Gimnasio> crearGimnasio(@RequestBody Gimnasio gimnasio) {
        Gimnasio nuevoGimnasio = gimnasioService.guardar(gimnasio);
        return ResponseEntity.ok(nuevoGimnasio);
    }
}
