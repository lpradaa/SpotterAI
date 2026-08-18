package com.spotterai.backend.controllers;

import com.spotterai.backend.dtos.UsuarioPerfilDTO;
import com.spotterai.backend.dtos.UsuarioRegistroDTO;
import com.spotterai.backend.dtos.UsuarioResponseDTO;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.services.UsuarioService;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController // Indica que esta clase responderá peticiones web devolviendo JSON
@RequestMapping("/api/usuarios") // Todas las rutas aquí empezarán por esto
public class UsuarioController {

    private final UsuarioService usuarioService;

    // Inyectamos el servicio
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Endpoint de Registro
     * POST http://localhost:8080/api/usuarios/registro
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody UsuarioRegistroDTO dto) {
        // El correo repetido y la contraseña corta los para el servicio con su
        // clave, y de eso responde ManejadorDeErrores. Capturar aqui y devolver
        // getMessage() enseñaria la clave en la pantalla de registro.
        return ResponseEntity.ok(usuarioService.registrarUsuario(dto));
    }

    /**
     * PUT /api/usuarios/idioma — en que idioma se te escribe.
     *
     * <p>Lo llama el selector de la cabecera. Todo lo demas que traduce el
     * backend sale de la cabecera Accept-Language de cada peticion; esto se
     * guarda porque los correos se mandan sin ninguna peticion de por medio.
     *
     * <p>Sin cuerpo de respuesta: no hay nada que devolver y el frontend ya sabe
     * lo que ha pedido. Si falla, tampoco pasa nada grave — se seguira
     * escribiendo en el idioma anterior.
     */
    @PutMapping("/idioma")
    public ResponseEntity<Void> guardarIdioma(@RequestBody Map<String, String> cuerpo) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        usuarioService.guardarIdioma(email, cuerpo.get("idioma"));

        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint para Actualizar Perfil
     * PUT http://localhost:8080/api/usuarios/perfil
     */
    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(@RequestBody UsuarioPerfilDTO dto) {
        // El email sale del token, no del cuerpo: es lo que impide editar el
        // perfil de otra persona mandando su id.
        String emailLogueado = SecurityContextHolder.getContext().getAuthentication().getName();

        return ResponseEntity.ok(usuarioService.actualizarPerfil(emailLogueado, dto));
    }

    /**
     * 🔥 NUEVO: Endpoint para Obtener Mis Datos Reales
     * GET http://localhost:8080/api/usuarios/perfil
     */
    @GetMapping("/perfil")
    public ResponseEntity<?> obtenerMiPerfil() {
        try {
            String emailLogueado = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // Llamamos al nuevo método que nos empaqueta todo, ¡incluyendo los horarios!
            Map<String, Object> perfilCompleto = usuarioService.obtenerMiPerfilCompleto(emailLogueado);
            
            return ResponseEntity.ok(perfilCompleto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al obtener el perfil: " + e.getMessage());
        }
    }

    /**
     * Endpoint para Buscar Compañeros (Matches)
     * GET http://localhost:8080/api/usuarios/matches
     */
    @GetMapping("/matches")
    public ResponseEntity<?> obtenerMatches() {
        try {
            // 1. Sacamos el email del usuario logueado (ej. Carlos) desde su Token
            String emailLogueado = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // 2. Llamamos al servicio para buscar sus matches
            List<UsuarioResponseDTO> posiblesCompañeros = usuarioService.buscarCompañeros(emailLogueado);
            
            // 3. Devolvemos la lista con un 200 OK
            return ResponseEntity.ok(posiblesCompañeros);
            
        } catch (RuntimeException e) {
            // Si no tiene gimnasio u ocurre un error, devolvemos un 400 Bad Request
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al buscar compañeros.");
        }
    }

    /**
     * De donde sale el numero: el desglose por factores, con su explicacion.
     * GET /api/usuarios/matches/{id}/explicacion
     *
     * <p>Va aparte de la lista porque solo hace falta cuando alguien pregunta por
     * una persona concreta, y calcularlo para los cien de Explorar en cada carga
     * seria pagarlo noventa y nueve veces de mas. (El comentario de antes decia
     * que "redactar cuesta una llamada al modelo": eso dejo de ser cierto cuando
     * la IA se aparco, y desde entonces esto es aritmetica sobre datos que ya
     * estan en memoria.)
     */
    @GetMapping("/matches/{id}/explicacion")
    public ResponseEntity<?> explicarMatch(@PathVariable Long id) {
        String emailLogueado = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(usuarioService.explicarMatch(emailLogueado, id));
    }

    /**
     * GET /api/usuarios/actividad
     *
     * Lo que han hecho últimamente tus compañeros. Solo los tuyos: la relación
     * aceptada es lo que da derecho a verlo.
     */
    @GetMapping("/actividad")
    public ResponseEntity<?> actividad() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(usuarioService.actividadDeCompaneros(email));
    }

    @GetMapping("/{otroUsuarioId}/perfil")
    public ResponseEntity<?> verPerfilDe(@PathVariable Long otroUsuarioId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(usuarioService.verPerfilDe(email, otroUsuarioId));
    }


    
}