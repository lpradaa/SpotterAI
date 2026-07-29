package com.spotterai.backend.avisos;

import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.models.Sesion;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lo que lee la persona que recibe el correo.
 *
 * <p>Lo que se rompe en un aviso casi nunca es el SMTP: es la frase. El nombre
 * que no aparece, la fecha en formato ISO, el enlace que apunta a localhost
 * porque nadie configuro la url base al desplegar. Todo eso se puede comprobar
 * sin servidor y sin base, y por eso el redactor no toca nada de fuera.
 */
class RedactorDeAvisosTest {

    /** La llave de baja no es lo que se prueba aqui; solo tiene que viajar. */
    private static final String LLAVE = "LLAVE-DE-PRUEBA";

    private final RedactorDeAvisos redactor = new RedactorDeAvisos("https://spotterai.example");

    private static Usuario usuario(Long id, String nombre, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(email);
        return u;
    }

    private static Solicitud solicitud() {
        Solicitud s = new Solicitud();
        s.setEmisor(usuario(1L, "Marta Ibáñez", "marta@test.com"));
        s.setReceptor(usuario(2L, "Luis", "luis@test.com"));
        return s;
    }

    private static Sesion sesion() {
        Gimnasio g = new Gimnasio();
        g.setNombre("McFit Madrid Centro");

        Sesion s = new Sesion();
        s.setProponente(usuario(1L, "Javi Ortega", "javi@test.com"));
        s.setInvitado(usuario(2L, "Luis", "luis@test.com"));
        s.setFecha(LocalDate.of(2026, 8, 3));
        s.setHoraInicio(LocalTime.of(18, 0));
        s.setHoraFin(LocalTime.of(20, 0));
        s.setGimnasio(g);
        return s;
    }

    @Test
    @DisplayName("El correo de una solicitud va a quien la recibe, no a quien la manda")
    void vaAlDestinatarioCorrecto() {
        // Con dos usuarios del mismo tipo en la misma clase, cruzarlos no da
        // error de compilacion: da un correo que llega a la persona equivocada
        // y le cuenta que ella misma quiere entrenar consigo.
        assertEquals("luis@test.com", redactor.paraSolicitud(solicitud(), LLAVE).para());
    }

    @Test
    @DisplayName("El asunto dice quién y qué: es lo único que se ve sin abrirlo")
    void elAsuntoSeExplicaSolo() {
        String asunto = redactor.paraSolicitud(solicitud(), LLAVE).asunto();

        assertTrue(asunto.contains("Marta Ibáñez"), asunto);
        // "Tienes una notificación" no dice si merece la pena abrirlo.
        assertTrue(asunto.toLowerCase().contains("entrenar"), asunto);
    }

    @Test
    @DisplayName("El enlace usa la url configurada, no localhost")
    void elEnlaceApuntaDondeDebe() {
        String cuerpo = redactor.paraSolicitud(solicitud(), LLAVE).cuerpo();

        // El fallo mas facil de cometer y el mas dificil de notar: desde la
        // maquina que manda los correos, un enlace a localhost abre bien.
        assertTrue(cuerpo.contains("https://spotterai.example/solicitudes"), cuerpo);
        assertFalse(cuerpo.contains("localhost"), cuerpo);
    }

    @Test
    @DisplayName("Una barra de más en la url no produce enlaces con //")
    void laBarraSobranteNoEnsucia() {
        String cuerpo = new RedactorDeAvisos("https://spotterai.example/")
                .paraSolicitud(solicitud(), LLAVE).cuerpo();

        assertTrue(cuerpo.contains("https://spotterai.example/solicitudes"), cuerpo);
        assertFalse(cuerpo.contains("example//"), cuerpo);
    }

    @Test
    @DisplayName("El de una sesión dice el día y la hora en cristiano")
    void laFechaSeLee() {
        Aviso aviso = redactor.paraSesion(sesion(), LLAVE);

        assertEquals("luis@test.com", aviso.para());
        assertTrue(aviso.cuerpo().contains("lunes 3 de agosto"), aviso.cuerpo());
        assertTrue(aviso.cuerpo().contains("18:00"), aviso.cuerpo());
        assertTrue(aviso.cuerpo().contains("20:00"), aviso.cuerpo());
        // Una fecha ISO en un correo es la señal de que nadie lo leyo antes de
        // mandarlo cien veces.
        assertFalse(aviso.cuerpo().contains("2026-08-03"), aviso.cuerpo());
    }

    @Test
    @DisplayName("El enlace de la sesión abre la conversación con quien la propone")
    void elEnlaceLlevaAlSitio() {
        assertTrue(redactor.paraSesion(sesion(), LLAVE).cuerpo()
                .contains("https://spotterai.example/conexiones?con=1"));
    }

    @Test
    @DisplayName("Sin gimnasio no se inventa uno ni se queda un 'en null'")
    void sinGimnasioNoSeInventa() {
        Sesion s = sesion();
        s.setGimnasio(null);

        String cuerpo = redactor.paraSesion(s, LLAVE).cuerpo();

        assertFalse(cuerpo.contains("null"), cuerpo);
        assertFalse(cuerpo.contains(", en "), cuerpo);
    }

    @Test
    @DisplayName("No se manda ningún dato del perfil del otro, ni la compatibilidad")
    void noSeCuentaNadaDeNadie() {
        // Un correo se reenvia, se lee en pantallas compartidas y se queda en
        // servidores que no son nuestros. "Marta, 93 % contigo" es contar de
        // Marta algo que Marta no ha decidido contar.
        Solicitud s = solicitud();
        s.setCompatibilidad(93);

        Aviso aviso = redactor.paraSolicitud(s, LLAVE);

        assertFalse(aviso.cuerpo().contains("93"), aviso.cuerpo());
        assertFalse(aviso.cuerpo().contains("%"), aviso.cuerpo());
        assertFalse(aviso.asunto().contains("93"), aviso.asunto());
    }
}
