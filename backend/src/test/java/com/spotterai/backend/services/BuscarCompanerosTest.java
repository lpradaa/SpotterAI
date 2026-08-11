package com.spotterai.backend.services;

import com.spotterai.backend.dtos.EstadoConCompanero;
import com.spotterai.backend.dtos.UsuarioResponseDTO;
import com.spotterai.backend.matching.ExplicadorCompatibilidad;
import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.DisponibilidadRepository;
import com.spotterai.backend.repositories.GimnasioRepository;
import com.spotterai.backend.repositories.SesionRepository;
import com.spotterai.backend.repositories.SolicitudRepository;
import com.spotterai.backend.repositories.BloqueoRepository;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalTime;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comportamiento del emparejamiento a nivel de servicio: orden por compatibilidad,
 * estado de la solicitud y numero de consultas.
 */
class BuscarCompanerosTest {

    private UsuarioRepository usuarioRepository;
    private DisponibilidadRepository disponibilidadRepository;
    private SolicitudRepository solicitudRepository;
    private UsuarioServiceImpl servicio;

    private Gimnasio gimnasio;
    private Usuario yo;

    @BeforeEach
    void preparar() {
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        disponibilidadRepository = Mockito.mock(DisponibilidadRepository.class);
        solicitudRepository = Mockito.mock(SolicitudRepository.class);

        servicio = new UsuarioServiceImpl(
                usuarioRepository,
                Mockito.mock(PasswordEncoder.class),
                Mockito.mock(GimnasioRepository.class),
                solicitudRepository,
                disponibilidadRepository,
                new ExplicadorCompatibilidad(),
                Mockito.mock(com.spotterai.backend.repositories.HitoRepository.class),
                Mockito.mock(com.spotterai.backend.repositories.EntrenamientoRepository.class),
                Mockito.mock(com.spotterai.backend.repositories.LevantamientoRepository.class),
                Mockito.mock(com.spotterai.backend.repositories.SesionRepository.class),
                Mockito.mock(com.spotterai.backend.repositories.BloqueoRepository.class),
                java.time.Clock.systemDefaultZone());

        gimnasio = new Gimnasio();
        gimnasio.setId(1L);
        gimnasio.setNombre("McFit Centro");

        yo = usuario(1L, "Luis", "Intermedio", "Hipertrofia", 28, gimnasio);
        when(usuarioRepository.findByEmail("luis@test.com")).thenReturn(Optional.of(yo));
        when(solicitudRepository.estadosPorCompanero(anyLong())).thenReturn(List.of());
    }

    private static Usuario usuario(Long id, String nombre, String nivel, String objetivo,
                                   Integer edad, Gimnasio gimnasio) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(nombre.toLowerCase() + "@test.com");
        u.setNivel(nivel);
        u.setObjetivos(objetivo);
        u.setEdad(edad);
        u.setGimnasio(gimnasio);
        return u;
    }

    private static Disponibilidad franja(Usuario u, String dia, String inicio, String fin) {
        return new Disponibilidad(dia, LocalTime.parse(inicio), LocalTime.parse(fin), u);
    }

    @Test
    @DisplayName("Los candidatos salen ordenados de mayor a menor compatibilidad")
    void ordenaPorCompatibilidad() {
        Usuario coincide = usuario(2L, "Marta", "Intermedio", "Hipertrofia", 29, gimnasio);
        Usuario noCoincide = usuario(3L, "Ana", "Avanzado", "Resistencia", 45, gimnasio);

        when(usuarioRepository.candidatosPara(1L)).thenReturn(List.of(noCoincide, coincide));
        // Todo el grupo en una consulta, yo incluido: el servicio carga los
        // perfiles de golpe y ya no pide lo mio por separado.
        when(disponibilidadRepository.findByUsuarioIdIn(any()))
                .thenReturn(List.of(
                        franja(yo, "Lunes", "18:00", "20:00"),
                        franja(coincide, "Lunes", "18:00", "20:00"),   // solapa entero
                        franja(noCoincide, "Domingo", "08:00", "10:00"))); // no solapa

        List<UsuarioResponseDTO> resultado = servicio.buscarCompañeros("luis@test.com");

        assertEquals(2, resultado.size());
        assertEquals("Marta", resultado.get(0).getNombre());
        assertTrue(resultado.get(0).getCompatibilidad() > resultado.get(1).getCompatibilidad());
        assertEquals(List.of("Lunes"), resultado.get(0).getDiasEnComun());
        assertEquals(120, resultado.get(0).getMinutosEnComun());
    }

    @Test
    @DisplayName("Un perfil incompleto no revienta ni penaliza: se puntua con lo que hay")
    void sinGimnasioNoLanzaExcepcion() {
        Usuario sinGimnasio = usuario(1L, "Luis", "Intermedio", "Hipertrofia", 28, null);
        when(usuarioRepository.findByEmail("luis@test.com")).thenReturn(Optional.of(sinGimnasio));
        when(usuarioRepository.candidatosPara(1L))
                .thenReturn(List.of(usuario(2L, "Marta", "Intermedio", "Hipertrofia", 29, gimnasio)));
        when(disponibilidadRepository.findByUsuarioId(1L)).thenReturn(List.of());
        when(disponibilidadRepository.findByUsuarioIdIn(any())).thenReturn(List.of());

        List<UsuarioResponseDTO> resultado = servicio.buscarCompañeros("luis@test.com");

        assertEquals(1, resultado.size());
        // Horario y gimnasio no se pueden evaluar y reparten su peso entre nivel,
        // objetivo y edad, que coinciden del todo. La nota sube respecto al 45 que
        // daba antes (no rellenar el perfil ya no penaliza como un mal encaje), pero
        // se queda lejos de 100 porque falta mas de la mitad de la evidencia.
        int compatibilidad = resultado.get(0).getCompatibilidad();
        assertTrue(compatibilidad > 45, "Deberia subir respecto al modelo anterior: %d".formatted(compatibilidad));
        assertTrue(compatibilidad < 80, "Con medio perfil no puede acercarse al maximo: %d".formatted(compatibilidad));
        assertTrue(resultado.get(0).getCompatibilidadIncompleta());
    }

    @Test
    @DisplayName("El estado de la solicitud llega al DTO en ambas direcciones")
    void reflejaEstadoDeLaSolicitud() {
        Usuario aceptado = usuario(2L, "Marta", "Intermedio", "Hipertrofia", 29, gimnasio);
        Usuario pendiente = usuario(3L, "Ana", "Intermedio", "Hipertrofia", 29, gimnasio);

        when(usuarioRepository.candidatosPara(1L)).thenReturn(List.of(aceptado, pendiente));
        when(disponibilidadRepository.findByUsuarioId(1L)).thenReturn(List.of());
        when(disponibilidadRepository.findByUsuarioIdIn(any())).thenReturn(List.of());

        // La proyeccion ya resuelve en SQL quien es "el otro", asi que aqui llega
        // el identificador del companero sin importar quien mando la solicitud.
        EstadoConCompanero ida = new EstadoConCompanero(aceptado.getId(), "ACEPTADA");
        EstadoConCompanero vuelta = new EstadoConCompanero(pendiente.getId(), "PENDIENTE");

        when(solicitudRepository.estadosPorCompanero(1L)).thenReturn(List.of(ida, vuelta));

        List<UsuarioResponseDTO> resultado = servicio.buscarCompañeros("luis@test.com");

        UsuarioResponseDTO marta = resultado.stream()
                .filter(d -> d.getNombre().equals("Marta")).findFirst().orElseThrow();
        UsuarioResponseDTO ana = resultado.stream()
                .filter(d -> d.getNombre().equals("Ana")).findFirst().orElseThrow();

        assertTrue(marta.getYaConectado());
        assertFalse(marta.getSolicitudPendiente());
        assertFalse(ana.getYaConectado());
        assertTrue(ana.getSolicitudPendiente());
    }

    @Test
    @DisplayName("El numero de consultas no crece con el numero de candidatos")
    void sinConsultasPorCandidato() {
        List<Usuario> muchos = List.of(
                usuario(2L, "A", "Intermedio", "Fuerza", 30, gimnasio),
                usuario(3L, "B", "Intermedio", "Fuerza", 30, gimnasio),
                usuario(4L, "C", "Intermedio", "Fuerza", 30, gimnasio),
                usuario(5L, "D", "Intermedio", "Fuerza", 30, gimnasio));

        when(usuarioRepository.candidatosPara(1L)).thenReturn(muchos);
        when(disponibilidadRepository.findByUsuarioId(1L)).thenReturn(List.of());
        when(disponibilidadRepository.findByUsuarioIdIn(any())).thenReturn(List.of());

        servicio.buscarCompañeros("luis@test.com");

        // Una sola consulta agrupada de horarios y una de solicitudes, con 4 candidatos.
        verify(disponibilidadRepository, times(1)).findByUsuarioIdIn(any());
        verify(solicitudRepository, times(1)).estadosPorCompanero(1L);
        // El metodo antiguo lanzaba dos consultas por candidato; ya no se usa.
        verify(solicitudRepository, Mockito.never())
                .findFirstByEmisorIdAndReceptorId(anyLong(), anyLong());

        // Y no se piden las Solicitud enteras: de cada una solo hacen falta el
        // identificador del otro y el estado, y traer la entidad arrastraba
        // emisor, receptor y los gimnasios de los dos. Con datos de demostracion
        // no se notaba —casi nadie tiene solicitudes— y crecia justo con lo que
        // se quiere que crezca: cuanta mas gente conectada, mas cara la lista.
        verify(solicitudRepository, Mockito.never()).findTodasPorUsuario(anyLong());

        // Los candidatos vienen con su gimnasio ya unido. Sin eso, Hibernate
        // resolvia la asociacion con un SELECT por gimnasio distinto del
        // catalogo, que crece con el numero de usuarios.
        verify(usuarioRepository, times(1)).candidatosPara(1L);
    }

    @Test
    @DisplayName("Sin candidatos se devuelve lista vacia sin tocar la base de datos")
    void sinCandidatos() {
        when(usuarioRepository.candidatosPara(1L)).thenReturn(List.of());

        assertTrue(servicio.buscarCompañeros("luis@test.com").isEmpty());
        verify(disponibilidadRepository, Mockito.never()).findByUsuarioIdIn(any());
    }
}
