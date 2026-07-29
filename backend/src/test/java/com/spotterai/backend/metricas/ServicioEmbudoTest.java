package com.spotterai.backend.metricas;

import com.spotterai.backend.matching.Tramo;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.SesionRepository;
import com.spotterai.backend.repositories.SolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** El cruce entre lo que se pidio y lo que acabo pasando. */
class ServicioEmbudoTest {

    private SolicitudRepository solicitudes;
    private SesionRepository sesiones;
    private ServicioEmbudo servicio;

    @BeforeEach
    void preparar() {
        solicitudes = Mockito.mock(SolicitudRepository.class);
        sesiones = Mockito.mock(SesionRepository.class);
        servicio = new ServicioEmbudo(solicitudes, sesiones);

        when(solicitudes.countByCompatibilidadIsNull()).thenReturn(0L);
        when(sesiones.paresQueEntrenaron()).thenReturn(List.of());
    }

    private static Usuario usuario(long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    private static Solicitud solicitud(long emisorId, long receptorId, int compatibilidad, String estado) {
        Solicitud s = new Solicitud();
        s.setEmisor(usuario(emisorId));
        s.setReceptor(usuario(receptorId));
        s.setCompatibilidad(compatibilidad);
        s.setEstado(estado);
        return s;
    }

    private Map<Tramo, EmbudoDeCompatibilidad.Fila> porTramo() {
        return servicio.calcular().filas().stream()
                .collect(Collectors.toMap(EmbudoDeCompatibilidad.Fila::tramo, Function.identity()));
    }

    @Test
    @DisplayName("Cada solicitud cae en el tramo de la puntuación con la que salió")
    void cadaUnaEnSuTramo() {
        when(solicitudes.findByCompatibilidadIsNotNull()).thenReturn(List.of(
                solicitud(1, 2, 90, "ACEPTADA"),
                solicitud(1, 3, 70, "PENDIENTE"),   // justo en el corte de alta
                solicitud(1, 4, 55, "RECHAZADA"),
                solicitud(1, 5, 20, "PENDIENTE")));

        Map<Tramo, EmbudoDeCompatibilidad.Fila> filas = porTramo();

        assertEquals(2, filas.get(Tramo.ALTA).enviadas());
        assertEquals(1, filas.get(Tramo.MEDIA).enviadas());
        assertEquals(1, filas.get(Tramo.BAJA).enviadas());
    }

    @Test
    @DisplayName("Un tramo sin solicitudes sale con ceros, no desaparece")
    void elTramoVacioSigueEstando() {
        when(solicitudes.findByCompatibilidadIsNotNull())
                .thenReturn(List.of(solicitud(1, 2, 90, "ACEPTADA")));

        // Que nadie haya escrito nunca a alguien de compatibilidad baja es un
        // dato sobre como se usa la aplicacion. Si la fila desaparece, la tabla
        // parece decir que todo el mundo escribe a todo el mundo.
        Map<Tramo, EmbudoDeCompatibilidad.Fila> filas = porTramo();

        assertEquals(3, filas.size());
        assertEquals(0, filas.get(Tramo.BAJA).enviadas());
    }

    @Test
    @DisplayName("Solo cuenta como entrenada si esa pareja confirmó una sesión")
    void soloLasQueEntrenaron() {
        when(solicitudes.findByCompatibilidadIsNotNull()).thenReturn(List.of(
                solicitud(1, 2, 90, "ACEPTADA"),
                solicitud(1, 3, 85, "ACEPTADA")));

        // Solo el par (1,2) llego a entrenar. El (1,3) se acepto y ahi se quedo,
        // que es el caso mas comun y el que hace que "aceptadas" sea una metrica
        // enganosa por si sola.
        when(sesiones.paresQueEntrenaron()).thenReturn(List.of(new ParDeUsuarios(1L, 2L)));

        EmbudoDeCompatibilidad.Fila alta = porTramo().get(Tramo.ALTA);

        assertEquals(2, alta.enviadas());
        assertEquals(2, alta.aceptadas());
        assertEquals(1, alta.entrenadas());
    }

    @Test
    @DisplayName("Da igual quién propuso la sesión: la pareja es la misma")
    void laParejaNoTieneOrden() {
        when(solicitudes.findByCompatibilidadIsNotNull())
                .thenReturn(List.of(solicitud(7, 3, 90, "ACEPTADA")));

        // La solicitud fue de 7 a 3 y la sesion la propuso 3. Sin normalizar el
        // par, esto contaria como que no entrenaron.
        when(sesiones.paresQueEntrenaron()).thenReturn(List.of(new ParDeUsuarios(3L, 7L)));

        assertEquals(1, porTramo().get(Tramo.ALTA).entrenadas());
    }

    @Test
    @DisplayName("Las solicitudes sin puntuación guardada no se cuelan: se cuentan aparte")
    void lasViejasSeDicen() {
        when(solicitudes.findByCompatibilidadIsNotNull())
                .thenReturn(List.of(solicitud(1, 2, 90, "ACEPTADA")));
        when(solicitudes.countByCompatibilidadIsNull()).thenReturn(37L);

        EmbudoDeCompatibilidad embudo = servicio.calcular();

        assertEquals(1, embudo.filas().stream().mapToLong(EmbudoDeCompatibilidad.Fila::enviadas).sum());
        assertEquals(37, embudo.sinPuntuacion());
    }

    @Test
    @DisplayName("Sin ninguna solicitud medible, el embudo existe y no concluye nada")
    void sinDatosNoConcluye() {
        when(solicitudes.findByCompatibilidadIsNotNull()).thenReturn(List.of());

        EmbudoDeCompatibilidad embudo = servicio.calcular();

        assertEquals(3, embudo.filas().size());
        assertFalse(embudo.sePuedeComparar());
        assertTrue(embudo.filas().stream().allMatch(f -> f.tasaAceptacion() == null));
    }
}
