package com.spotterai.backend.services;

import com.spotterai.backend.semantica.VectorDeBiografia;
import com.spotterai.backend.textos.TextosDePrueba;
import com.spotterai.backend.dtos.ActividadDTO;
import com.spotterai.backend.matching.ExplicadorCompatibilidad;
import com.spotterai.backend.models.Entrenamiento;
import com.spotterai.backend.models.Hito;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Lo que han hecho tus compañeros.
 *
 * <p>Lo que se protege aquí es sobre todo a quién <em>no</em> se le enseña. Es la
 * única pantalla donde aparecen datos de otras personas sin que ellas hayan
 * hecho nada para mostrártelos, así que la frontera —la relación aceptada— es lo
 * que hay que fijar. Un fallo aquí no se ve: sale una lista, y parece bien.
 */
class ActividadDeCompanerosTest {

    /** Miércoles 1 de julio de 2026. */
    private static final LocalDate HOY = LocalDate.of(2026, 7, 1);

    private UsuarioRepository usuarioRepository;
    private SolicitudRepository solicitudRepository;
    private HitoRepository hitoRepository;
    private EntrenamientoRepository entrenamientoRepository;
    private UsuarioServiceImpl servicio;

    private final Usuario yo = persona(1L, "Luis", "luis@test.com");
    private final Usuario companero = persona(2L, "Ana", "ana@test.com");
    private final Usuario desconocido = persona(3L, "Nadie", "nadie@test.com");

    private static Usuario persona(Long id, String nombre, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(email);
        return u;
    }

    private static Hito hito(Usuario de, String titulo, LocalDate fecha) {
        Hito h = new Hito();
        h.setUsuario(de);
        h.setTitulo(titulo);
        h.setFecha(fecha);
        return h;
    }

    private static Entrenamiento entrenamiento(Usuario de, String tipo, LocalDate fecha) {
        Entrenamiento e = new Entrenamiento();
        e.setUsuario(de);
        e.setTipo(tipo);
        e.setFecha(fecha);
        return e;
    }

    @BeforeEach
    void preparar() {
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        solicitudRepository = Mockito.mock(SolicitudRepository.class);
        hitoRepository = Mockito.mock(HitoRepository.class);
        entrenamientoRepository = Mockito.mock(EntrenamientoRepository.class);

        Clock congelado = Clock.fixed(
                HOY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

        servicio = new UsuarioServiceImpl(
                usuarioRepository,
                Mockito.mock(PasswordEncoder.class),
                Mockito.mock(GimnasioRepository.class),
                solicitudRepository,
                Mockito.mock(DisponibilidadRepository.class),
                new ExplicadorCompatibilidad(TextosDePrueba.nuevo()),
                hitoRepository,
                entrenamientoRepository,
                Mockito.mock(LevantamientoRepository.class),
                Mockito.mock(SesionRepository.class),
                Mockito.mock(BloqueoRepository.class),
                TextosDePrueba.nuevo(),
                new VectorDeBiografia(new com.spotterai.backend.semantica.ServicioDeEmbeddings("")),
                congelado);

        when(usuarioRepository.findByEmail("luis@test.com")).thenReturn(Optional.of(yo));
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(companero));

        // Ana es compañera; con "desconocido" no hay nada.
        Solicitud aceptada = new Solicitud();
        aceptada.setEmisor(yo);
        aceptada.setReceptor(companero);
        aceptada.setEstado("ACEPTADA");
        when(solicitudRepository.findAceptadasPorUsuario(1L)).thenReturn(List.of(aceptada));

        when(hitoRepository.findByUsuarioIdInOrderByFechaDescIdDesc(any())).thenReturn(List.of());
        when(entrenamientoRepository
                .findByUsuarioIdInAndFechaGreaterThanEqualOrderByFechaDesc(any(), any()))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("Sin compañeros no hay actividad, y no se consulta nada más")
    void sinCompanerosNoHayNada() {
        when(solicitudRepository.findAceptadasPorUsuario(1L)).thenReturn(List.of());

        assertTrue(servicio.actividadDeCompaneros("luis@test.com").isEmpty());

        // Ni siquiera se pregunta: sin nadie a quien mirar, la consulta sobra.
        Mockito.verify(hitoRepository, Mockito.never())
                .findByUsuarioIdInOrderByFechaDescIdDesc(any());
    }

    @Test
    @DisplayName("Solo se pregunta por los compañeros, nunca por todo el mundo")
    void soloSePreguntaPorLosCompaneros() {
        servicio.actividadDeCompaneros("luis@test.com");

        // La frontera va en la consulta y no en un filtro posterior: si se
        // pidieran los hitos de todos y se descartaran después, cualquier despiste
        // al tocar el filtro convertiría esto en un muro público.
        Mockito.verify(hitoRepository).findByUsuarioIdInOrderByFechaDescIdDesc(List.of(2L));
        Mockito.verify(entrenamientoRepository)
                .findByUsuarioIdInAndFechaGreaterThanEqualOrderByFechaDesc(
                        List.of(2L), HOY.minusDays(UsuarioServiceImpl.DIAS_DE_ACTIVIDAD));
    }

    @Test
    @DisplayName("Los hitos y los entrenamientos se mezclan por fecha, no por tipo")
    void seMezclanPorFecha() {
        when(hitoRepository.findByUsuarioIdInOrderByFechaDescIdDesc(any()))
                .thenReturn(List.of(hito(companero, "100 en banca", HOY.minusDays(5))));
        when(entrenamientoRepository
                .findByUsuarioIdInAndFechaGreaterThanEqualOrderByFechaDesc(any(), any()))
                .thenReturn(List.of(entrenamiento(companero, "Fuerza", HOY.minusDays(1))));

        List<ActividadDTO> actividad = servicio.actividadDeCompaneros("luis@test.com");

        // Vienen de dos consultas ordenadas por separado. Sin reordenarlas juntas
        // saldrían todos los hitos primero, que no es "lo más reciente".
        assertEquals(2, actividad.size());
        assertEquals(ActividadDTO.ENTRENAMIENTO, actividad.get(0).tipo());
        assertEquals(ActividadDTO.HITO, actividad.get(1).tipo());
    }

    @Test
    @DisplayName("Lo viejo no cuenta: 'últimamente' deja de significar algo si cabe todo")
    void loViejoNoCuenta() {
        when(hitoRepository.findByUsuarioIdInOrderByFechaDescIdDesc(any())).thenReturn(List.of(
                hito(companero, "De esta semana", HOY.minusDays(2)),
                hito(companero, "De hace dos meses", HOY.minusDays(60))));

        List<ActividadDTO> actividad = servicio.actividadDeCompaneros("luis@test.com");

        assertEquals(1, actividad.size());
        assertEquals("De esta semana", actividad.get(0).titulo());
    }

    @Test
    @DisplayName("Un hito sin fecha no revienta la lista")
    void hitoSinFecha() {
        when(hitoRepository.findByUsuarioIdInOrderByFechaDescIdDesc(any())).thenReturn(List.of(
                hito(companero, "Sin fecha", null),
                hito(companero, "Con fecha", HOY.minusDays(1))));

        List<ActividadDTO> actividad = servicio.actividadDeCompaneros("luis@test.com");

        assertEquals(1, actividad.size());
        assertEquals("Con fecha", actividad.get(0).titulo());
    }

    @Test
    @DisplayName("La lista tiene tope: es una señal de vida, no un muro")
    void hayTope() {
        List<Hito> muchos = new java.util.ArrayList<>();
        for (int i = 0; i < UsuarioServiceImpl.MAX_ACTIVIDAD + 10; i++) {
            muchos.add(hito(companero, "Marca " + i, HOY.minusDays(1)));
        }
        when(hitoRepository.findByUsuarioIdInOrderByFechaDescIdDesc(any())).thenReturn(muchos);

        assertEquals(UsuarioServiceImpl.MAX_ACTIVIDAD,
                servicio.actividadDeCompaneros("luis@test.com").size());
    }

    @Test
    @DisplayName("Cada entrada dice de quién es, para poder ir a su perfil")
    void seSabeDeQuienEs() {
        when(hitoRepository.findByUsuarioIdInOrderByFechaDescIdDesc(any()))
                .thenReturn(List.of(hito(companero, "100 en banca", HOY.minusDays(1))));

        ActividadDTO entrada = servicio.actividadDeCompaneros("luis@test.com").get(0);

        assertEquals(2L, entrada.usuarioId());
        assertEquals("Ana", entrada.usuarioNombre());
    }
}
