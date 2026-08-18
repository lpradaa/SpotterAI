package com.spotterai.backend.controllers;

import com.spotterai.backend.services.AlmacenDeMedios;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Subida y entrega de fotos y vídeos.
 *
 * La entrega es pública a propósito: las rutas llevan un UUID, así que no se
 * adivinan, y exigir el token aquí impediría usar las URL directamente en un
 * {@code <img>} o un {@code <video>}, que no pueden mandar cabeceras. Es la
 * misma limitación del navegador que obligó a los tickets del canal de eventos.
 */
@RestController
@RequestMapping("/api/medios")
public class MedioController {

    private final AlmacenDeMedios almacen;

    public MedioController(AlmacenDeMedios almacen) {
        this.almacen = almacen;
    }

    @PostMapping
    public ResponseEntity<?> subir(@RequestParam("archivo") MultipartFile archivo) {
        try {
            AlmacenDeMedios.MedioGuardado guardado = almacen.guardar(archivo);
            return ResponseEntity.ok(Map.of("url", guardado.url(), "tipo", guardado.tipo()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body("No se ha podido guardar el archivo.");
        }
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<Resource> servir(@PathVariable String nombre) {
        try {
            Path ruta = almacen.localizar(nombre);
            if (!Files.isRegularFile(ruta)) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(almacen.tipoDeContenido(nombre)))
                    // El contenido de una ruta nunca cambia, porque el nombre lo
                    // genera la subida: se puede cachear sin miedo.
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                    // Que el navegador no adivine el tipo. Sin esto, un fichero
                    // que colara la validacion podria acabar interpretado como
                    // HTML y ejecutando algo en nuestro dominio.
                    .header("X-Content-Type-Options", "nosniff")
                    .header("Content-Disposition", "inline")
                    .body(new FileSystemResource(ruta));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
