package com.spotterai.backend.seguridad;

import com.spotterai.backend.models.Reporte;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.ReporteRepository;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Que la aplicación se entere de que alguien se ha portado mal.
 *
 * <p>Bloquear te protege a ti; esto es lo que junta los hechos de varias
 * personas contra la misma. Lo que se fija aquí es que el hecho quede escrito
 * con quien lo pone y a quién, que no haga falta que sea la primera vez para
 * poder registrarlo, y que un motivo inventado no se cuele.
 */
class ReportesTest {

    private ReporteRepository reportes;
    private UsuarioRepository usuarios;
    private Reportes servicio;

    private static final Instant AHORA = Instant.parse("2026-08-10T12:00:00Z");

    private final Usuario yo = usuario(1L, "yo@test.com", "Yo");
    private final Usuario otro = usuario(2L, "otro@test.com", "Otro");

    private static Usuario usuario(Long id, String email, String nombre) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        u.setNombre(nombre);
        return u;
    }

    @BeforeEach
    void preparar() {
        reportes = mock(ReporteRepository.class);
        usuarios = mock(UsuarioRepository.class);
        Clock reloj = Clock.fixed(AHORA, ZoneId.systemDefault());
        servicio = new Reportes(reportes, usuarios, reloj);

        when(usuarios.findByEmail("yo@test.com")).thenReturn(Optional.of(yo));
        when(usuarios.findById(2L)).thenReturn(Optional.of(otro));
        when(reportes.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Registra quién reporta a quién, con el motivo y la fecha")
    void registraElReporte() {
        servicio.reportar("yo@test.com", 2L, "ACOSO", "Me ha escrito insistiendo tras bloquearlo yo antes");

        ArgumentCaptor<Reporte> capturado = ArgumentCaptor.forClass(Reporte.class);
        verify(reportes).save(capturado.capture());

        Reporte r = capturado.getValue();
        // Cruzar reportador y reportado no da error de compilación: da un
        // reporte que dice lo contrario de lo que pasó.
        assertEquals(yo, r.getReportador());
        assertEquals(otro, r.getReportado());
        assertEquals("ACOSO", r.getMotivo());
        assertEquals(AHORA.atZone(ZoneId.systemDefault()).toLocalDateTime(), r.getCreadoEn());
    }

    @Test
    @DisplayName("El detalle en blanco se guarda como null, no como cadena vacía")
    void elDetalleEnBlancoEsNull() {
        servicio.reportar("yo@test.com", 2L, "SPAM", "   ");

        ArgumentCaptor<Reporte> capturado = ArgumentCaptor.forClass(Reporte.class);
        verify(reportes).save(capturado.capture());

        assertNull(capturado.getValue().getDetalle());
    }

    @Test
    @DisplayName("No puedes reportarte a ti mismo")
    void noPuedesReportarteATiMismo() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.reportar("yo@test.com", 1L, "SPAM", null));

        verify(reportes, never()).save(any());
    }

    @Test
    @DisplayName("Un motivo que no existe se rechaza")
    void unMotivoInventadoSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.reportar("yo@test.com", 2L, "ME_CAE_MAL", null));

        verify(reportes, never()).save(any());
    }

    @Test
    @DisplayName("La misma pareja puede tener varios reportes: no es un estado, es un registro")
    void laMismaParejaPuedeTenerVariosReportes() {
        servicio.reportar("yo@test.com", 2L, "SPAM", null);
        servicio.reportar("yo@test.com", 2L, "ACOSO", "Otra vez, ahora con amenazas");

        // A diferencia de Bloqueo, aquí no hay restricción de unicidad: cada
        // reporte es un hecho aparte, aunque sea de la misma pareja.
        verify(reportes, times(2)).save(any());
    }

    @Test
    @DisplayName("todos() devuelve lo que dé el repositorio, ya ordenado")
    void todosDelegaEnElRepositorio() {
        List<Reporte> lista = List.of(new Reporte(yo, otro, "SPAM", null, null));
        when(reportes.findAllByOrderByCreadoEnDesc()).thenReturn(lista);

        assertEquals(lista, servicio.todos());
    }

    @Test
    @DisplayName("Reportar a alguien que no existe falla, y no a quien no está autenticado")
    void reportarAQuienNoExiste() {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> servicio.reportar("yo@test.com", 99L, "SPAM", null));
    }
}
