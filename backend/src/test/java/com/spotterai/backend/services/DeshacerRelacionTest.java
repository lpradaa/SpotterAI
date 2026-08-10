package com.spotterai.backend.services;

import com.spotterai.backend.eventos.CanalEventos;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.SolicitudRepository;
import com.spotterai.backend.repositories.BloqueoRepository;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Deshacer una relacion tiene una regla que no es obvia: puedes retirar lo que
 * enviaste tu, pero no puedes hacer desaparecer una solicitud que te han
 * mandado, porque eso es responderla sin que el otro se entere.
 */
class DeshacerRelacionTest {

    private SolicitudRepository solicitudRepository;
    private UsuarioRepository usuarioRepository;
    private SolicitudServiceImpl servicio;

    private final Usuario yo = usuario(1L, "yo@test.com");
    private final Usuario otro = usuario(2L, "otro@test.com");

    private static Usuario usuario(Long id, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        u.setNombre("Usuario " + id);
        return u;
    }

    private static Solicitud relacion(Usuario emisor, Usuario receptor, String estado) {
        Solicitud s = new Solicitud();
        s.setId(99L);
        s.setEmisor(emisor);
        s.setReceptor(receptor);
        s.setEstado(estado);
        return s;
    }

    private final UsuarioService usuarioService = Mockito.mock(UsuarioService.class);

    private final BloqueoRepository bloqueoRepository = Mockito.mock(BloqueoRepository.class);

    @BeforeEach
    void preparar() {
        solicitudRepository = Mockito.mock(SolicitudRepository.class);
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        servicio = new SolicitudServiceImpl(solicitudRepository, usuarioRepository, new CanalEventos(),
                usuarioService, bloqueoRepository);

        Mockito.when(usuarioRepository.findByEmail(yo.getEmail())).thenReturn(Optional.of(yo));
        Mockito.when(solicitudRepository.findFirstByEmisorIdAndReceptorId(any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Puedes retirar una solicitud que enviaste tu y sigue pendiente")
    void retirarLaPropiaPendiente() {
        Solicitud mia = relacion(yo, otro, "PENDIENTE");
        Mockito.when(solicitudRepository.findFirstByEmisorIdAndReceptorId(yo.getId(), otro.getId()))
                .thenReturn(Optional.of(mia));

        servicio.deshacerRelacion(yo.getEmail(), otro.getId());

        // Se borra, no se marca: con la restriccion unica, dejar la fila
        // impediria volver a conectar con esa persona nunca mas.
        Mockito.verify(solicitudRepository).delete(mia);
    }

    @Test
    @DisplayName("No puedes hacer desaparecer una solicitud que te han enviado a ti")
    void noSePuedeRetirarLaAjenaPendiente() {
        Solicitud suya = relacion(otro, yo, "PENDIENTE");
        Mockito.when(solicitudRepository.findFirstByEmisorIdAndReceptorId(otro.getId(), yo.getId()))
                .thenReturn(Optional.of(suya));

        // Eso es responderla a escondidas: para eso estan aceptar y rechazar,
        // que ademas avisan a quien la mando.
        assertThrows(SecurityException.class,
                () -> servicio.deshacerRelacion(yo.getEmail(), otro.getId()));

        Mockito.verify(solicitudRepository, Mockito.never()).delete(any());
    }

    @Test
    @DisplayName("Cualquiera de los dos puede deshacer una relacion ya aceptada")
    void cualquieraDeshaceLaAceptada() {
        Solicitud aceptada = relacion(otro, yo, "ACEPTADA");
        Mockito.when(solicitudRepository.findFirstByEmisorIdAndReceptorId(otro.getId(), yo.getId()))
                .thenReturn(Optional.of(aceptada));

        servicio.deshacerRelacion(yo.getEmail(), otro.getId());

        assertDoesNotThrow(() -> {});
        Mockito.verify(solicitudRepository).delete(aceptada);
    }

    @Test
    @DisplayName("Se encuentra la relacion aunque la enviara el otro")
    void seBuscaEnLosDosSentidos() {
        Solicitud aceptada = relacion(otro, yo, "ACEPTADA");
        Mockito.when(solicitudRepository.findFirstByEmisorIdAndReceptorId(yo.getId(), otro.getId()))
                .thenReturn(Optional.empty());
        Mockito.when(solicitudRepository.findFirstByEmisorIdAndReceptorId(otro.getId(), yo.getId()))
                .thenReturn(Optional.of(aceptada));

        servicio.deshacerRelacion(yo.getEmail(), otro.getId());

        Mockito.verify(solicitudRepository).delete(aceptada);
    }

    @Test
    @DisplayName("Sin relacion previa no hay nada que deshacer")
    void sinRelacion() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.deshacerRelacion(yo.getEmail(), otro.getId()));
    }
}
