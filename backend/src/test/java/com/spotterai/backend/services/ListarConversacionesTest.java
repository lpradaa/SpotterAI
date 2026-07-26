package com.spotterai.backend.services;

import com.spotterai.backend.dtos.ConversacionDTO;
import com.spotterai.backend.dtos.SinLeerPorEmisor;
import com.spotterai.backend.eventos.CanalEventos;
import com.spotterai.backend.models.Mensaje;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.MensajeRepository;
import com.spotterai.backend.repositories.SolicitudRepository;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La lista de compañeros mezcla tres fuentes: quienes son tus compañeros, cual
 * fue el ultimo mensaje de cada conversacion y cuanto queda sin leer. Lo que se
 * rompe en silencio al tocarla es el orden y que no se caiga nadie por el
 * camino, asi que es lo que se fija aqui.
 */
class ListarConversacionesTest {

    private MensajeRepository mensajeRepository;
    private UsuarioRepository usuarioRepository;
    private SolicitudRepository solicitudRepository;
    private MensajeServiceImpl servicio;

    private final Usuario yo = usuario(1L, "Yo");
    private final Usuario ana = usuario(2L, "Ana");
    private final Usuario bruno = usuario(3L, "Bruno");
    private final Usuario carla = usuario(4L, "Carla");

    private static Usuario usuario(Long id, String nombre) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(nombre.toLowerCase() + "@test.com");
        return u;
    }

    private static Solicitud aceptada(Usuario emisor, Usuario receptor) {
        Solicitud s = new Solicitud();
        s.setEmisor(emisor);
        s.setReceptor(receptor);
        s.setEstado("ACEPTADA");
        return s;
    }

    private static Mensaje mensaje(Long id, Usuario de, Usuario para, String texto, LocalDateTime cuando) {
        Mensaje m = new Mensaje();
        m.setId(id);
        m.setEmisor(de);
        m.setReceptor(para);
        m.setContenido(texto);
        // fechaEnvio se pone en el constructor, hay que sobreescribirla por reflexion
        // no hace falta: el DTO la lee de aqui, asi que basta con un setter propio.
        try {
            var campo = Mensaje.class.getDeclaredField("fechaEnvio");
            campo.setAccessible(true);
            campo.set(m, cuando);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return m;
    }

    @BeforeEach
    void preparar() {
        mensajeRepository = Mockito.mock(MensajeRepository.class);
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        solicitudRepository = Mockito.mock(SolicitudRepository.class);
        servicio = new MensajeServiceImpl(mensajeRepository, usuarioRepository,
                solicitudRepository, new CanalEventos());

        Mockito.when(usuarioRepository.findByEmail(yo.getEmail())).thenReturn(Optional.of(yo));
        Mockito.when(solicitudRepository.findAceptadasPorUsuario(yo.getId()))
                .thenReturn(List.of(aceptada(yo, ana), aceptada(bruno, yo), aceptada(yo, carla)));
        Mockito.when(mensajeRepository.ultimoMensajePorConversacion(yo.getId())).thenReturn(List.of());
        Mockito.when(mensajeRepository.contarSinLeerPorEmisor(yo.getId())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Un compañero con el que aun no has hablado sale igual en la lista")
    void companeroSinMensajes() {
        List<ConversacionDTO> lista = servicio.listarConversaciones(yo.getEmail());

        // Es justo cuando mas falta hace verlo: acabas de conectar y hay que
        // escribir el primer mensaje.
        assertEquals(3, lista.size());
        assertTrue(lista.stream().allMatch(c -> c.ultimoMensaje() == null));
        assertTrue(lista.stream().allMatch(c -> c.sinLeer() == 0));
    }

    @Test
    @DisplayName("Las conversaciones vivas van primero, de mas reciente a mas antigua")
    void ordenPorActividad() {
        LocalDateTime base = LocalDateTime.of(2026, 3, 1, 10, 0);
        Mockito.when(mensajeRepository.ultimoMensajePorConversacion(yo.getId())).thenReturn(List.of(
                mensaje(10L, ana, yo, "hace un rato", base.plusHours(5)),
                mensaje(11L, yo, bruno, "hace mucho", base)));

        List<ConversacionDTO> lista = servicio.listarConversaciones(yo.getEmail());

        assertEquals("Ana", lista.get(0).nombre());
        assertEquals("Bruno", lista.get(1).nombre());
        assertEquals("Carla", lista.get(2).nombre(), "Sin mensajes, al final");
    }

    @Test
    @DisplayName("Se distingue quien envio el ultimo mensaje")
    void quienEnvioElUltimo() {
        LocalDateTime cuando = LocalDateTime.of(2026, 3, 1, 10, 0);
        Mockito.when(mensajeRepository.ultimoMensajePorConversacion(yo.getId())).thenReturn(List.of(
                mensaje(10L, yo, ana, "lo dije yo", cuando),
                mensaje(11L, bruno, yo, "lo dijo el", cuando.minusHours(1))));

        List<ConversacionDTO> lista = servicio.listarConversaciones(yo.getEmail());

        assertTrue(buscar(lista, "Ana").mioElUltimo(), "Para poder anteponer 'Tú:'");
        assertFalse(buscar(lista, "Bruno").mioElUltimo());
    }

    @Test
    @DisplayName("Lo sin leer se cuenta por compañero, no en bloque")
    void sinLeerPorCompanero() {
        Mockito.when(mensajeRepository.contarSinLeerPorEmisor(yo.getId()))
                .thenReturn(List.of(new SinLeerPorEmisor(ana.getId(), 3L)));

        List<ConversacionDTO> lista = servicio.listarConversaciones(yo.getEmail());

        assertEquals(3, buscar(lista, "Ana").sinLeer());
        assertEquals(0, buscar(lista, "Bruno").sinLeer());
    }

    @Test
    @DisplayName("Un compañero duplicado en las solicitudes sale una sola vez")
    void companeroDuplicado() {
        // La restriccion unica evita los duplicados nuevos, pero la consulta
        // devuelve las dos direcciones y no conviene fiarse de que no se crucen.
        Mockito.when(solicitudRepository.findAceptadasPorUsuario(yo.getId()))
                .thenReturn(List.of(aceptada(yo, ana), aceptada(ana, yo)));

        List<ConversacionDTO> lista = servicio.listarConversaciones(yo.getEmail());

        assertEquals(1, lista.size());
    }

    private static ConversacionDTO buscar(List<ConversacionDTO> lista, String nombre) {
        return lista.stream().filter(c -> c.nombre().equals(nombre)).findFirst().orElseThrow();
    }
}
