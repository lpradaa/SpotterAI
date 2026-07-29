package com.spotterai.backend.avisos;

import com.spotterai.backend.models.Sesion;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.SesionRepository;
import com.spotterai.backend.repositories.SolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * A quién se le escribe y, sobre todo, a quién no.
 *
 * <p>Un aviso por correo es lo más fácil de convertir en molestia: en cuanto
 * llegan dos por algo que ya estaba resuelto, la gente silencia el remitente y
 * entonces se pierden también los que servían. Lo que se fija aquí es que no se
 * mande dos veces, que no se mande de lo ya contestado y que un fallo de envío
 * no arrastre a los demás.
 */
class RastreadorDeAvisosTest {

    private SolicitudRepository solicitudes;
    private SesionRepository sesiones;
    private Cartero cartero;
    private RastreadorDeAvisos rastreador;

    private final List<Aviso> enviados = new ArrayList<>();

    /** Reloj fijo: si el barrido depende de la hora real, la prueba parpadea. */
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 29, 12, 0);
    private final Clock reloj = Clock.fixed(
            AHORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    private static Usuario usuario(Long id, String nombre, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(email);
        return u;
    }

    private static Solicitud solicitud() {
        Solicitud s = new Solicitud();
        s.setId(1L);
        s.setEmisor(usuario(1L, "Marta", "marta@test.com"));
        s.setReceptor(usuario(2L, "Luis", "luis@test.com"));
        s.setEstado("PENDIENTE");
        return s;
    }

    private static Sesion sesion() {
        Sesion s = new Sesion();
        s.setId(5L);
        s.setProponente(usuario(1L, "Javi", "javi@test.com"));
        s.setInvitado(usuario(2L, "Luis", "luis@test.com"));
        s.setFecha(LocalDate.of(2026, 8, 3));
        s.setHoraInicio(LocalTime.of(18, 0));
        s.setHoraFin(LocalTime.of(20, 0));
        return s;
    }

    @BeforeEach
    void preparar() {
        solicitudes = Mockito.mock(SolicitudRepository.class);
        sesiones = Mockito.mock(SesionRepository.class);

        cartero = enviados::add;

        rastreador = new RastreadorDeAvisos(solicitudes, sesiones,
                new RedactorDeAvisos("https://spotterai.example"), cartero, reloj);

        when(solicitudes.pendientesPorAvisar(any(), any())).thenReturn(List.of());
        when(sesiones.propuestasPorAvisar(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Manda un correo por cada solicitud que sigue esperando")
    void avisaDeLoPendiente() {
        when(solicitudes.pendientesPorAvisar(any(), any())).thenReturn(List.of(solicitud()));

        rastreador.barrer();

        assertEquals(1, enviados.size());
        assertEquals("luis@test.com", enviados.get(0).para());
    }

    @Test
    @DisplayName("Después de mandarlo queda marcado, para que no salga otra vez")
    void marcaLoAvisado() {
        Solicitud s = solicitud();
        when(solicitudes.pendientesPorAvisar(any(), any())).thenReturn(List.of(s));

        rastreador.barrer();

        // La marca va en la fila y no en una variable: el barrido corre cada
        // minuto y tiene que sobrevivir a reinicios sin repetir avisos.
        assertEquals(AHORA, s.getAvisadoEn());
    }

    @Test
    @DisplayName("Si el envío falla, NO se marca: el siguiente barrido lo reintenta")
    void loQueFallaSeReintenta() {
        Solicitud s = solicitud();
        when(solicitudes.pendientesPorAvisar(any(), any())).thenReturn(List.of(s));
        rastreador = new RastreadorDeAvisos(solicitudes, sesiones,
                new RedactorDeAvisos("https://spotterai.example"),
                aviso -> { throw new IllegalStateException("SMTP caído"); }, reloj);

        rastreador.barrer();

        assertNull(s.getAvisadoEn(),
                "Marcarlo tras un fallo deja a alguien sin enterarse para siempre");
    }

    @Test
    @DisplayName("Un envío que revienta no impide los siguientes")
    void unFalloNoTumbaLaTanda() {
        Solicitud rota = solicitud();
        rota.setReceptor(usuario(3L, "Rota", "rebota@test.com"));
        Solicitud buena = solicitud();

        when(solicitudes.pendientesPorAvisar(any(), any())).thenReturn(List.of(rota, buena));
        rastreador = new RastreadorDeAvisos(solicitudes, sesiones,
                new RedactorDeAvisos("https://spotterai.example"),
                aviso -> {
                    if (aviso.para().equals("rebota@test.com")) throw new IllegalStateException("rebota");
                    enviados.add(aviso);
                }, reloj);

        assertDoesNotThrow(() -> rastreador.barrer());

        // Si la primera direccion de la tanda tumbara el barrido, bastaria una
        // cuenta muerta para que nadie mas recibiera avisos nunca.
        assertEquals(1, enviados.size());
        assertEquals("luis@test.com", enviados.get(0).para());
        assertNull(rota.getAvisadoEn());
        assertEquals(AHORA, buena.getAvisadoEn());
    }

    @Test
    @DisplayName("Las propuestas de sesión también se avisan")
    void avisaDeLasSesiones() {
        when(sesiones.propuestasPorAvisar(any(), any(), any())).thenReturn(List.of(sesion()));

        rastreador.barrer();

        assertEquals(1, enviados.size());
        assertTrue(enviados.get(0).cuerpo().contains("lunes 3 de agosto"));
    }

    @Test
    @DisplayName("Sin nada pendiente no se manda ningún correo")
    void sinNadaNoMolesta() {
        rastreador.barrer();

        assertTrue(enviados.isEmpty());
    }

    @Test
    @DisplayName("La ventana pedida excluye lo recién creado y lo demasiado viejo")
    void pideLaVentanaCorrecta() {
        rastreador.barrer();

        // Lo recien creado se deja fuera para que quien lo esta viendo en
        // directo pueda responder sin recibir un correo inutil; lo viejo, para
        // no soltar una tanda a todo el mundo al desplegar esto por primera vez.
        Mockito.verify(solicitudes).pendientesPorAvisar(
                AHORA.minus(RastreadorDeAvisos.CADUCIDAD),
                AHORA.minus(RastreadorDeAvisos.DEMORA));
    }

    @Test
    @DisplayName("De las sesiones solo se piden las de un día que aún no ha pasado")
    void lasSesionesPasadasNoSeAvisan() {
        rastreador.barrer();

        // Avisar el miercoles de una propuesta para el martes no le sirve a
        // nadie, y encima invita a responder algo que ya no se puede responder.
        Mockito.verify(sesiones).propuestasPorAvisar(any(), any(), Mockito.eq(AHORA.toLocalDate()));
    }
}
