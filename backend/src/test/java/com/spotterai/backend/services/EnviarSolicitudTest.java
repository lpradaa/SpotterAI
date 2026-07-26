package com.spotterai.backend.services;

import com.spotterai.backend.eventos.CanalEventos;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.SolicitudRepository;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Reglas de enviar una solicitud.
 *
 * La comprobacion previa y la restriccion unica de la base cubren cosas
 * distintas: la primera da un mensaje claro en el caso normal, la segunda es la
 * unica que puede parar dos peticiones simultaneas. Aqui se fija que las dos se
 * lean igual desde fuera.
 */
class EnviarSolicitudTest {

    private SolicitudRepository solicitudRepository;
    private UsuarioRepository usuarioRepository;
    private SolicitudServiceImpl servicio;

    private final Usuario emisor = usuario(1L, "yo@test.com");
    private final Usuario receptor = usuario(2L, "otro@test.com");

    private static Usuario usuario(Long id, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        u.setNombre("Usuario " + id);
        return u;
    }

    @BeforeEach
    void preparar() {
        solicitudRepository = Mockito.mock(SolicitudRepository.class);
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        servicio = new SolicitudServiceImpl(solicitudRepository, usuarioRepository, new CanalEventos());

        Mockito.when(usuarioRepository.findByEmail(emisor.getEmail())).thenReturn(Optional.of(emisor));
        Mockito.when(usuarioRepository.findById(receptor.getId())).thenReturn(Optional.of(receptor));
        Mockito.when(solicitudRepository.findFirstByEmisorIdAndReceptorId(any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Una solicitud nueva se guarda y se devuelve")
    void solicitudNueva() {
        Mockito.when(solicitudRepository.save(any())).thenAnswer(inv -> {
            Solicitud s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        var dto = servicio.enviarSolicitud(emisor.getEmail(), receptor.getId());

        assertEquals("PENDIENTE", dto.getEstado());
        assertEquals(receptor.getId(), dto.getReceptorId());
    }

    @Test
    @DisplayName("Chocar con la restriccion unica se lee como duplicado, no como error del servidor")
    void carreraEntreDosPeticiones() {
        // Las dos peticiones ven que no hay nada y las dos intentan insertar: la
        // segunda la para la base. Si esto se escapara, saldria un 500 y quien lo
        // recibe no entenderia nada.
        Mockito.when(solicitudRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("uk_solicitud_emisor_receptor"));

        var error = assertThrows(IllegalArgumentException.class,
                () -> servicio.enviarSolicitud(emisor.getEmail(), receptor.getId()));

        assertTrue(error.getMessage().contains("Ya existe una solicitud"));
    }

    @Test
    @DisplayName("No puedes enviarte una solicitud a ti mismo")
    void solicitudAUnoMismo() {
        Mockito.when(usuarioRepository.findById(emisor.getId())).thenReturn(Optional.of(emisor));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.enviarSolicitud(emisor.getEmail(), emisor.getId()));

        Mockito.verify(solicitudRepository, Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Una solicitud ya existente en sentido contrario tambien se rechaza")
    void solicitudEnSentidoContrario() {
        // La restriccion unica de la base no cubre el par invertido, asi que esta
        // comprobacion es la unica que lo para.
        Solicitud previa = new Solicitud();
        previa.setEmisor(receptor);
        previa.setReceptor(emisor);
        Mockito.when(solicitudRepository.findFirstByEmisorIdAndReceptorId(receptor.getId(), emisor.getId()))
                .thenReturn(Optional.of(previa));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.enviarSolicitud(emisor.getEmail(), receptor.getId()));

        Mockito.verify(solicitudRepository, Mockito.never()).save(any());
    }
}
