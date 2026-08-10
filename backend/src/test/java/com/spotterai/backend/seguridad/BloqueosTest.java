package com.spotterai.backend.seguridad;

import com.spotterai.backend.models.Bloqueo;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.BloqueoRepository;
import com.spotterai.backend.repositories.SolicitudRepository;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Quitarte a alguien de encima.
 *
 * <p>Esta aplicación le enseña a un desconocido en qué gimnasio entrenas y a qué
 * horas exactas. Eso es lo que convierte una molestia en algo físico, y hasta
 * ahora no había forma de cortar: «deshacer relación» solo borraba la fila de la
 * solicitud, así que la otra persona podía mandarte otra al segundo siguiente.
 */
class BloqueosTest {

    private BloqueoRepository bloqueos;
    private UsuarioRepository usuarios;
    private SolicitudRepository solicitudes;
    private Bloqueos servicio;

    private final Usuario yo = usuario(1L, "yo@test.com");
    private final Usuario otro = usuario(2L, "otro@test.com");

    private static Usuario usuario(Long id, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        u.setNombre("Usuario " + id);
        return u;
    }

    @BeforeEach
    void preparar() {
        bloqueos = mock(BloqueoRepository.class);
        usuarios = mock(UsuarioRepository.class);
        solicitudes = mock(SolicitudRepository.class);

        servicio = new Bloqueos(bloqueos, usuarios, solicitudes,
                Clock.fixed(Instant.parse("2026-08-10T10:00:00Z"), ZoneId.systemDefault()));

        when(usuarios.findByEmail("yo@test.com")).thenReturn(Optional.of(yo));
        when(usuarios.findById(2L)).thenReturn(Optional.of(otro));
        when(bloqueos.findByBloqueadorIdAndBloqueadoId(any(), any())).thenReturn(Optional.empty());
        when(solicitudes.findFirstByEmisorIdAndReceptorId(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Bloquear guarda el bloqueo en el sentido correcto")
    void bloqueaEnElSentidoCorrecto() {
        servicio.bloquear("yo@test.com", 2L);

        ArgumentCaptor<Bloqueo> guardado = ArgumentCaptor.forClass(Bloqueo.class);
        verify(bloqueos).save(guardado.capture());

        // Cruzarlos no da error de compilación: da un bloqueo que echa a la
        // persona equivocada y deja entrar a la que querías fuera.
        assertEquals(yo, guardado.getValue().getBloqueador());
        assertEquals(otro, guardado.getValue().getBloqueado());
    }

    @Test
    @DisplayName("Bloquear deshace la relación que hubiera")
    void bloquearDeshaceLaRelacion() {
        Solicitud relacion = new Solicitud();
        relacion.setEmisor(otro);
        relacion.setReceptor(yo);
        when(solicitudes.findFirstByEmisorIdAndReceptorId(1L, 2L)).thenReturn(Optional.empty());
        when(solicitudes.findFirstByEmisorIdAndReceptorId(2L, 1L)).thenReturn(Optional.of(relacion));

        servicio.bloquear("yo@test.com", 2L);

        // Si la relación siguiera en pie, el bloqueado conservaría el chat
        // abierto: bloquear a alguien que puede seguir escribiéndote no es
        // bloquear.
        verify(solicitudes).delete(relacion);
    }

    @Test
    @DisplayName("Bloquear dos veces no falla ni duplica")
    void esIdempotente() {
        when(bloqueos.findByBloqueadorIdAndBloqueadoId(1L, 2L))
                .thenReturn(Optional.of(new Bloqueo(yo, otro, LocalDateTime.now())));

        // Dos pestañas, o dos clics. Quien bloquea quiere que quede bloqueado; un
        // error de "ya lo estaba" solo estorba.
        assertDoesNotThrow(() -> servicio.bloquear("yo@test.com", 2L));
        verify(bloqueos, never()).save(any());
    }

    @Test
    @DisplayName("No puedes bloquearte a ti mismo")
    void aTiMismoNo() {
        assertThrows(IllegalArgumentException.class, () -> servicio.bloquear("yo@test.com", 1L));
        verify(bloqueos, never()).save(any());
    }

    @Test
    @DisplayName("Desbloquear quita solo el tuyo, no el que te pusieron a ti")
    void desbloquearQuitaSoloElTuyo() {
        Bloqueo mio = new Bloqueo(yo, otro, LocalDateTime.now());
        when(bloqueos.findByBloqueadorIdAndBloqueadoId(1L, 2L)).thenReturn(Optional.of(mio));

        servicio.desbloquear("yo@test.com", 2L);

        verify(bloqueos).delete(mio);
        // Solo se busca el tuyo: si esto quitara también el de la otra dirección,
        // podrías desbloquearte a ti mismo de la lista de alguien.
        verify(bloqueos, never()).findByBloqueadorIdAndBloqueadoId(2L, 1L);
    }

    @Test
    @DisplayName("Desbloquear no devuelve la relación que había")
    void desbloquearNoRestauraLaRelacion() {
        when(bloqueos.findByBloqueadorIdAndBloqueadoId(1L, 2L))
                .thenReturn(Optional.of(new Bloqueo(yo, otro, LocalDateTime.now())));

        servicio.desbloquear("yo@test.com", 2L);

        // Restaurarla sola pondría a alguien a hablar otra vez con quien bloqueó
        // sin haberlo pedido. Volver a conectar se decide entre los dos.
        verify(solicitudes, never()).save(any());
    }

    @Test
    @DisplayName("Desbloquear a quien no habías bloqueado no hace nada")
    void desbloquearAQuienNoEstaba() {
        assertDoesNotThrow(() -> servicio.desbloquear("yo@test.com", 2L));
        verify(bloqueos, never()).delete(any(Bloqueo.class));
    }
}
