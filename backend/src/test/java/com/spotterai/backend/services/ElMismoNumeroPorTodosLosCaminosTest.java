package com.spotterai.backend.services;

import com.spotterai.backend.semantica.VectorDeBiografia;
import com.spotterai.backend.textos.TextosDePrueba;
import com.spotterai.backend.dtos.ConteoPorUsuario;
import com.spotterai.backend.dtos.UsuarioResponseDTO;
import com.spotterai.backend.matching.Ejercicio;
import com.spotterai.backend.matching.ExplicadorCompatibilidad;
import com.spotterai.backend.models.*;
import com.spotterai.backend.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * La misma pareja vale lo mismo se mire por donde se mire.
 *
 * <p>Hay tres sitios donde sale un porcentaje: la lista de gente, la ficha de
 * una persona y la explicación redactada. Los tres han estado desincronizados en
 * algún momento, y siempre por el mismo motivo: cada uno montaba por su cuenta
 * los datos que le pasaba al motor, y al añadir un factor se actualizaban dos de
 * los tres.
 *
 * <ul>
 *   <li>El tablero decía 90 y Explorar 83 de la misma persona, porque Explorar
 *       puntuaba sin levantamientos.
 *   <li>La lista decía 56 y la explicación 48, porque la explicación puntuaba
 *       sin constancia.
 * </ul>
 *
 * <p>Las dos veces se arregló la llamada. Esta prueba existe para que la tercera
 * salte aquí: no comprueba un número concreto —eso cambia cada vez que se
 * reajustan los pesos— sino que los tres caminos digan el mismo, sea cual sea.
 */
class ElMismoNumeroPorTodosLosCaminosTest {

    private UsuarioRepository usuarioRepository;
    private UsuarioServiceImpl servicio;

    private final Usuario yo = persona(1L, "Luis", "luis@test.com");
    private final Usuario otro = persona(2L, "Ana", "ana@test.com");

    private static Usuario persona(Long id, String nombre, String email) {
        Gimnasio g = new Gimnasio();
        g.setId(1L);
        g.setNombre("McFit");

        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(email);
        u.setNivel("Intermedio");
        u.setObjetivos("Hipertrofia");
        u.setEdad(28);
        u.setRutina("TORSO_PIERNA");
        u.setGimnasio(g);
        return u;
    }

    private static Disponibilidad franja(Usuario de) {
        return new Disponibilidad("Lunes", LocalTime.of(18, 0), LocalTime.of(20, 0), de, true);
    }

    private static Levantamiento marca(Usuario de, double peso) {
        Levantamiento l = new Levantamiento();
        l.setUsuario(de);
        l.setEjercicio(Ejercicio.PRESS_BANCA);
        l.setPeso(peso);
        l.setRepeticiones(5);
        return l;
    }

    @BeforeEach
    void preparar() {
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        DisponibilidadRepository disponibilidades = Mockito.mock(DisponibilidadRepository.class);
        LevantamientoRepository levantamientos = Mockito.mock(LevantamientoRepository.class);
        EntrenamientoRepository entrenamientos = Mockito.mock(EntrenamientoRepository.class);
        SolicitudRepository solicitudes = Mockito.mock(SolicitudRepository.class);

        servicio = new UsuarioServiceImpl(
                usuarioRepository,
                Mockito.mock(PasswordEncoder.class),
                Mockito.mock(GimnasioRepository.class),
                solicitudes,
                disponibilidades,
                new ExplicadorCompatibilidad(TextosDePrueba.nuevo()),
                Mockito.mock(HitoRepository.class),
                entrenamientos,
                levantamientos,
                Mockito.mock(SesionRepository.class),
                Mockito.mock(BloqueoRepository.class),
                TextosDePrueba.nuevo(),
                new VectorDeBiografia(new com.spotterai.backend.semantica.ServicioDeEmbeddings("")),
                Clock.systemDefaultZone());

        when(usuarioRepository.findByEmail("luis@test.com")).thenReturn(Optional.of(yo));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(otro));
        when(usuarioRepository.candidatosPara(1L)).thenReturn(List.of(otro));
        when(solicitudes.estadosPorCompanero(any())).thenReturn(List.of());
        when(solicitudes.findAceptadasPorUsuario(any())).thenReturn(List.of());

        // Todos los datos que mira el motor, para que ninguno se quede "sin
        // datos": si faltara alguno, los tres caminos coincidirian en un numero
        // descontado y la prueba pasaria sin comprobar lo que quiere comprobar.
        when(disponibilidades.findByUsuarioIdIn(any()))
                .thenReturn(List.of(franja(yo), franja(otro)));
        when(levantamientos.findByUsuarioIdIn(any()))
                .thenReturn(List.of(marca(yo, 100), marca(otro, 95)));
        when(entrenamientos.contarDesde(any(), any())).thenReturn(List.of(
                new ConteoPorUsuario(1L, 12L), new ConteoPorUsuario(2L, 12L)));
        when(entrenamientos.contarTotales(any())).thenReturn(List.of(
                new ConteoPorUsuario(1L, 40L), new ConteoPorUsuario(2L, 40L)));
        when(entrenamientos.countByUsuarioIdAndFechaGreaterThanEqual(any(), any())).thenReturn(0L);

        when(Mockito.mock(HitoRepository.class).findByUsuarioIdOrderByFechaDescIdDesc(any()))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("La lista, la ficha y la explicación dan el mismo porcentaje")
    void losTresCoinciden() {
        UsuarioResponseDTO enLaLista = servicio.buscarCompañeros("luis@test.com").get(0);
        int enLaFicha = servicio.verPerfilDe("luis@test.com", 2L).compatibilidad();
        String titular = servicio.explicarMatch("luis@test.com", 2L).titular();

        assertEquals(enLaLista.getCompatibilidad(), enLaFicha,
                "La lista y la ficha de la misma persona tienen que decir lo mismo");

        // El titular es "Ana - 87% de compatibilidad": se saca el numero de ahi
        // porque es literalmente lo que lee quien abre la explicacion.
        assertEquals(enLaLista.getCompatibilidad() + "%",
                titular.replaceAll(".*?(\\d+%).*", "$1"),
                "La explicación decía un número distinto del de la lista: " + titular);
    }
}
