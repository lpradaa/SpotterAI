package com.spotterai.backend.services;

import com.spotterai.backend.dtos.UsuarioResponseDTO;
import com.spotterai.backend.matching.Ejercicio;
import com.spotterai.backend.matching.ExplicadorCompatibilidad;
import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.models.Levantamiento;
import com.spotterai.backend.models.Usuario;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * La misma pareja vale lo mismo mire donde mire.
 *
 * <p>El tablero y Explorar cruzan a la misma gente, pero Explorar llamaba a la
 * version de la calculadora que puntua sin levantamientos. Sin marcas, el factor
 * de fuerza queda "sin datos": su peso se reparte y entra el descuento por
 * evidencia, de modo que la misma persona aparecia con un porcentaje en una
 * pantalla y otro distinto en la otra.
 *
 * <p>Nadie lo vio porque cada pantalla, por separado, daba un numero razonable.
 * Solo se nota mirando las dos a la vez, que es justo lo que hace esta prueba.
 */
class LasDosListasCoincidenTest {

    private UsuarioRepository usuarioRepository;
    private DisponibilidadRepository disponibilidadRepository;
    private LevantamientoRepository levantamientoRepository;
    private SolicitudRepository solicitudRepository;
    private UsuarioServiceImpl servicio;

    private final Gimnasio gimnasio = new Gimnasio();
    private final Usuario yo = new Usuario();
    private final Usuario otro = new Usuario();

    private static Usuario persona(Usuario u, Long id, String nombre, String email) {
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(email);
        u.setNivel("Intermedio");
        u.setObjetivos("Hipertrofia");
        u.setEdad(28);
        // Perfil completo: si faltara cualquier otro factor, la puntuacion
        // saldria incompleta por ese y la prueba de la fuerza no probaria nada.
        u.setRutina("TORSO_PIERNA");
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
        disponibilidadRepository = Mockito.mock(DisponibilidadRepository.class);
        levantamientoRepository = Mockito.mock(LevantamientoRepository.class);
        solicitudRepository = Mockito.mock(SolicitudRepository.class);

        servicio = new UsuarioServiceImpl(
                usuarioRepository,
                Mockito.mock(PasswordEncoder.class),
                Mockito.mock(GimnasioRepository.class),
                solicitudRepository,
                disponibilidadRepository,
                new ExplicadorCompatibilidad(),
                Mockito.mock(HitoRepository.class),
                Mockito.mock(EntrenamientoRepository.class),
                levantamientoRepository,
                Mockito.mock(SesionRepository.class),
                Clock.systemDefaultZone());

        gimnasio.setId(1L);
        gimnasio.setNombre("McFit Centro");

        persona(yo, 1L, "Luis", "luis@test.com").setGimnasio(gimnasio);
        persona(otro, 2L, "Ana", "ana@test.com").setGimnasio(gimnasio);

        when(usuarioRepository.findByEmail("luis@test.com")).thenReturn(Optional.of(yo));
        when(usuarioRepository.findByIdNot(1L)).thenReturn(List.of(otro));
        when(solicitudRepository.findTodasPorUsuario(any())).thenReturn(List.of());

        when(disponibilidadRepository.findByUsuarioId(1L)).thenReturn(List.of(franja(yo)));
        when(disponibilidadRepository.findByUsuarioId(2L)).thenReturn(List.of(franja(otro)));
        when(disponibilidadRepository.findByUsuarioIdIn(any()))
                .thenReturn(List.of(franja(otro)));

        // Los dos mueven casi lo mismo: la fuerza deberia sumar, no quedarse fuera.
        when(levantamientoRepository.findByUsuarioId(1L)).thenReturn(List.of(marca(yo, 100)));
        when(levantamientoRepository.findByUsuarioId(2L)).thenReturn(List.of(marca(otro, 95)));
        when(levantamientoRepository.findByUsuarioIdIn(any())).thenReturn(List.of(marca(otro, 95)));
    }

    @Test
    @DisplayName("El tablero y Explorar dan el mismo porcentaje para la misma persona")
    void mismaPersonaMismoNumero() {
        UsuarioResponseDTO enElTablero = servicio.buscarCompañeros("luis@test.com").get(0);
        UsuarioResponseDTO enExplorar = servicio.explorarComunidad("luis@test.com").get(0);

        assertEquals(enElTablero.getCompatibilidad(), enExplorar.getCompatibilidad(),
                "Dos numeros para la misma pareja es peor que un numero malo: "
                        + "quien lo ve no sabe cual creer");
    }

    @Test
    @DisplayName("Explorar cuenta la fuerza: no puede quedarse 'sin datos' teniendo marcas")
    void explorarNoIgnoraLasMarcas() {
        UsuarioResponseDTO enExplorar = servicio.explorarComunidad("luis@test.com").get(0);

        // Si el factor de fuerza se quedara fuera, la puntuacion vendria marcada
        // como incompleta y con el descuento por evidencia aplicado.
        assertNotEquals(Boolean.TRUE, enExplorar.getCompatibilidadIncompleta(),
                "Con marcas en los dos lados, la fuerza tiene datos de sobra");
    }

    @Test
    @DisplayName("Las dos listas dicen si podéis cubriros")
    void lasDosDicenSiOsCubris() {
        assertEquals(Boolean.TRUE, servicio.buscarCompañeros("luis@test.com").get(0).getFuerzaCompatible());
        assertEquals(Boolean.TRUE, servicio.explorarComunidad("luis@test.com").get(0).getFuerzaCompatible());
    }

    @Test
    @DisplayName("Sin ejercicios en común no se dice que no podéis: se dice que no se sabe")
    void sinDatosNoEsUnNo() {
        // El otro solo tiene sentadilla; yo, banca. No hay con que comparar.
        Levantamiento suyo = new Levantamiento();
        suyo.setUsuario(otro);
        suyo.setEjercicio(Ejercicio.SENTADILLA);
        suyo.setPeso(140.0);
        suyo.setRepeticiones(3);
        when(levantamientoRepository.findByUsuarioIdIn(any())).thenReturn(List.of(suyo));

        assertNull(servicio.buscarCompañeros("luis@test.com").get(0).getFuerzaCompatible(),
                "Null es 'no se sabe'. False seria afirmar que no podeis, que es otra cosa");
    }

    @Test
    @DisplayName("Todo lo que la tarjeta necesita llega en las dos listas")
    void nadaSeQuedaPorElCamino() {
        // El patron que ha mordido cinco veces: un campo que existe, se guarda y
        // no llega a verse. Biografia, metaSemanal, fotoUrl dos veces y rutina.
        // La primera vez se arreglo el sintoma; esto es para que la sexta salte
        // aqui y no meses despues.
        otro.setBiografia("Busco a alguien constante");
        otro.setFotoUrl("/api/medios/ana.png");
        otro.setAvatar("ciruela");

        for (UsuarioResponseDTO dto : List.of(
                servicio.buscarCompañeros("luis@test.com").get(0),
                servicio.explorarComunidad("luis@test.com").get(0))) {

            assertNotNull(dto.getNombre());
            assertNotNull(dto.getNivel());
            assertNotNull(dto.getObjetivos());
            assertNotNull(dto.getRutina());
            assertNotNull(dto.getGimnasioNombre());
            assertNotNull(dto.getBiografia());
            assertNotNull(dto.getFotoUrl());
            assertNotNull(dto.getAvatar());
            assertNotNull(dto.getEdad());
            assertNotNull(dto.getCompatibilidad());
            assertNotNull(dto.getResumenCompatibilidad());
            assertNotNull(dto.getFranjasEnComun());
        }
    }

    @Test
    @DisplayName("La rutina llega a las listas para poder pintarla")
    void laRutinaViaja() {
        assertEquals("Torso / Pierna", servicio.buscarCompañeros("luis@test.com").get(0).getRutina());
        assertEquals("Torso / Pierna", servicio.explorarComunidad("luis@test.com").get(0).getRutina());
    }
}
