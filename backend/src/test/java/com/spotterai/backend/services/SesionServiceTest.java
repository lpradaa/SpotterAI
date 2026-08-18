package com.spotterai.backend.services;

import com.spotterai.backend.dtos.NuevaSesionDTO;
import com.spotterai.backend.dtos.SesionDTO;
import com.spotterai.backend.dtos.SugerenciaSesionDTO;
import com.spotterai.backend.eventos.CanalEventos;
import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Entrenamiento;
import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.models.Sesion;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.DisponibilidadRepository;
import com.spotterai.backend.repositories.EntrenamientoRepository;
import com.spotterai.backend.repositories.GimnasioRepository;
import com.spotterai.backend.repositories.SesionRepository;
import com.spotterai.backend.repositories.SolicitudRepository;
import com.spotterai.backend.repositories.UsuarioRepository;
import com.spotterai.backend.textos.ErrorDeNegocio;
import com.spotterai.backend.textos.Textos;
import com.spotterai.backend.textos.TextosDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Clock;
import java.util.Locale;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Reglas de quedar para entrenar.
 *
 * El reloj esta congelado en un miercoles a las diez, asi que "el viernes" y "el
 * lunes pasado" significan siempre lo mismo. Sin eso, la mitad de estas pruebas
 * pasarian o fallarian segun el dia en que se ejecutaran, que es la peor clase
 * de prueba: la que un dia se pone roja sola y enseña a ignorarla.
 */
class SesionServiceTest {

    /** Miércoles 1 de julio de 2026, 10:00. */
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 1, 10, 0);

    private SesionRepository sesionRepository;
    private UsuarioRepository usuarioRepository;
    private SolicitudRepository solicitudRepository;
    private DisponibilidadRepository disponibilidadRepository;
    private EntrenamientoRepository entrenamientoRepository;
    private GimnasioRepository gimnasioRepository;
    private SesionServiceImpl servicio;

    private final Gimnasio gimnasio = gimnasio(1L, "McFit");
    private final Usuario yo = usuario(1L, "yo@test.com", "Luis");
    private final Usuario otro = usuario(2L, "otro@test.com", "Ana");
    private final Usuario desconocido = usuario(3L, "nadie@test.com", "Nadie");

    private static Usuario usuario(Long id, String email, String nombre) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        u.setNombre(nombre);
        return u;
    }

    private static Gimnasio gimnasio(Long id, String nombre) {
        Gimnasio g = new Gimnasio();
        g.setId(id);
        g.setNombre(nombre);
        return g;
    }

    @BeforeEach
    void preparar() {
        sesionRepository = Mockito.mock(SesionRepository.class);
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        solicitudRepository = Mockito.mock(SolicitudRepository.class);
        disponibilidadRepository = Mockito.mock(DisponibilidadRepository.class);
        entrenamientoRepository = Mockito.mock(EntrenamientoRepository.class);
        gimnasioRepository = Mockito.mock(GimnasioRepository.class);

        Clock congelado = Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());

        servicio = new SesionServiceImpl(sesionRepository, usuarioRepository, solicitudRepository,
                disponibilidadRepository, entrenamientoRepository, gimnasioRepository,
                new CanalEventos(), congelado);

        for (Usuario u : List.of(yo, otro, desconocido)) {
            Mockito.when(usuarioRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
            Mockito.when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));
        }

        // Yo y "otro" somos compañeros; con "desconocido" no hay nada.
        Solicitud aceptada = new Solicitud();
        aceptada.setEmisor(yo);
        aceptada.setReceptor(otro);
        aceptada.setEstado("ACEPTADA");
        Mockito.when(solicitudRepository.findAceptadasPorUsuario(yo.getId()))
                .thenReturn(List.of(aceptada));

        Mockito.when(disponibilidadRepository.findByUsuarioId(Mockito.any())).thenReturn(List.of());
        Mockito.when(sesionRepository.propuestaViva(Mockito.any(), Mockito.any()))
                .thenReturn(Optional.empty());
        Mockito.when(sesionRepository.save(any(Sesion.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private NuevaSesionDTO datos(String fecha, String inicio, String fin) {
        return new NuevaSesionDTO(fecha, inicio, fin, null, null);
    }

    /** Igual, pero eligiendo donde quedar. */
    private NuevaSesionDTO datosEn(String fecha, String inicio, String fin, Long gimnasioId) {
        return new NuevaSesionDTO(fecha, inicio, fin, null, gimnasioId);
    }

    private Sesion sesionGuardada(String fecha, String inicio, String fin, String estado) {
        Sesion s = new Sesion();
        s.setId(10L);
        s.setProponente(yo);
        s.setInvitado(otro);
        s.setFecha(LocalDate.parse(fecha));
        s.setHoraInicio(LocalTime.parse(inicio));
        s.setHoraFin(LocalTime.parse(fin));
        s.setEstado(estado);
        Mockito.when(sesionRepository.findById(10L)).thenReturn(Optional.of(s));
        return s;
    }

    // --- Proponer ---

    @Test
    @DisplayName("Se puede proponer a un compañero un dia por delante")
    void proponerNormal() {
        SesionDTO dto = servicio.proponer(yo.getEmail(), otro.getId(),
                datos("2026-07-03", "18:00", "20:00"));

        assertEquals(Sesion.PROPUESTA, dto.estado());
        assertTrue(dto.mia());
        assertEquals("Ana", dto.conNombre());
        // Quien propone no puede responderse a si mismo.
        assertFalse(dto.puedoResponder());
    }

    @Test
    @DisplayName("Quedar exige ser compañeros, igual que escribirse")
    void soloEntreCompaneros() {
        assertThrows(SecurityException.class, () -> servicio.proponer(
                yo.getEmail(), desconocido.getId(), datos("2026-07-03", "18:00", "20:00")));
    }

    @Test
    @DisplayName("No se puede quedar con uno mismo")
    void niConmigoMismo() {
        assertThrows(IllegalArgumentException.class, () -> servicio.proponer(
                yo.getEmail(), yo.getId(), datos("2026-07-03", "18:00", "20:00")));
    }

    @Test
    @DisplayName("No se puede quedar en el pasado")
    void niEnElPasado() {
        ErrorDeNegocio e = assertThrows(ErrorDeNegocio.class,
                () -> servicio.proponer(yo.getEmail(), otro.getId(),
                        datos("2026-06-29", "18:00", "20:00")));

        // Por la clave y no por un trozo de la frase: la frase es lo que cambia
        // de idioma, así que afirmando sobre ella este test decidiría en qué
        // idioma tiene que estar escrito el servicio.
        assertEquals("error.sesion.enElPasado", e.mensaje().clave());
    }

    /**
     * Lo que se le enseña a alguien sale en su idioma.
     *
     * <p>Estos errores acaban pintados tal cual en la pantalla —el controlador
     * devuelve el texto y el frontend lo enseña—, así que con la frase escrita
     * dentro de la excepción salía en español en una pantalla en inglés.
     */
    @Test
    @DisplayName("Y se redacta en el idioma de quien pregunta")
    void elErrorSeTraduce() {
        ErrorDeNegocio e = assertThrows(ErrorDeNegocio.class,
                () -> servicio.proponer(yo.getEmail(), otro.getId(),
                        datos("2026-06-29", "18:00", "20:00")));

        Textos textos = TextosDePrueba.nuevo();
        assertEquals("No se puede quedar en el pasado.", textos.de(e.mensaje()));
        assertEquals("You cannot meet in the past.", textos.de(e.mensaje(), Locale.ENGLISH));
    }

    @Test
    @DisplayName("Una hora de hoy que ya ha pasado tambien es el pasado")
    void niHaceUnRato() {
        assertThrows(IllegalArgumentException.class, () -> servicio.proponer(
                yo.getEmail(), otro.getId(), datos("2026-07-01", "08:00", "10:00")));
    }

    @Test
    @DisplayName("Una sesion de cinco minutos o de ocho horas es un error de tecleo")
    void duracionesImposibles() {
        assertThrows(IllegalArgumentException.class, () -> servicio.proponer(
                yo.getEmail(), otro.getId(), datos("2026-07-03", "18:00", "18:05")));

        assertThrows(IllegalArgumentException.class, () -> servicio.proponer(
                yo.getEmail(), otro.getId(), datos("2026-07-03", "10:00", "20:00")));
    }

    @Test
    @DisplayName("Solo una propuesta viva a la vez con la misma persona")
    void unaPropuestaCadaVez() {
        Mockito.when(sesionRepository.propuestaViva(yo.getId(), otro.getId()))
                .thenReturn(Optional.of(new Sesion()));

        assertThrows(IllegalArgumentException.class, () -> servicio.proponer(
                yo.getEmail(), otro.getId(), datos("2026-07-03", "18:00", "20:00")));
    }

    @Test
    @DisplayName("Si los dos entrenan en el mismo gimnasio, la sesion ya sabe donde es")
    void elGimnasioSeDeduce() {
        yo.setGimnasio(gimnasio);
        otro.setGimnasio(gimnasio);

        SesionDTO dto = servicio.proponer(yo.getEmail(), otro.getId(),
                datos("2026-07-03", "18:00", "20:00"));

        assertEquals("McFit", dto.gimnasioNombre());

        yo.setGimnasio(null);
        otro.setGimnasio(null);
    }

    // --- Responder ---

    @Test
    @DisplayName("Solo responde quien esta invitado")
    void soloElInvitadoResponde() {
        sesionGuardada("2026-07-03", "18:00", "20:00", Sesion.PROPUESTA);

        // La propuse yo, asi que aceptarla yo no tiene sentido.
        assertThrows(SecurityException.class,
                () -> servicio.responder(yo.getEmail(), 10L, true));

        SesionDTO dto = servicio.responder(otro.getEmail(), 10L, true);
        assertEquals(Sesion.ACEPTADA, dto.estado());
    }

    @Test
    @DisplayName("Una propuesta ya resuelta no se vuelve a responder")
    void noSeRespondeDosVeces() {
        sesionGuardada("2026-07-03", "18:00", "20:00", Sesion.ACEPTADA);

        assertThrows(IllegalArgumentException.class,
                () -> servicio.responder(otro.getEmail(), 10L, true));
    }

    @Test
    @DisplayName("Aceptar una hora que ya paso la cierra en vez de dejar un plan imposible")
    void aceptarTarde() {
        Sesion vieja = sesionGuardada("2026-06-29", "18:00", "20:00", Sesion.PROPUESTA);

        assertThrows(IllegalArgumentException.class,
                () -> servicio.responder(otro.getEmail(), 10L, true));

        assertEquals(Sesion.CANCELADA, vieja.getEstado(),
                "La propuesta caducada tiene que quedar cerrada, no pendiente para siempre");
    }

    // --- Cancelar ---

    @Test
    @DisplayName("Cualquiera de los dos puede cancelar mientras no haya empezado")
    void cancelaCualquiera() {
        Sesion s = sesionGuardada("2026-07-03", "18:00", "20:00", Sesion.ACEPTADA);

        servicio.cancelar(otro.getEmail(), 10L);

        assertEquals(Sesion.CANCELADA, s.getEstado());
    }

    @Test
    @DisplayName("Alguien de fuera no puede cancelar una sesion ajena")
    void nadieDeFuera() {
        sesionGuardada("2026-07-03", "18:00", "20:00", Sesion.ACEPTADA);

        assertThrows(SecurityException.class, () -> servicio.cancelar(desconocido.getEmail(), 10L));
    }

    // --- Confirmar: aqui es donde se cierra el bucle ---

    @Test
    @DisplayName("Confirmar una sesion pasada la apunta como entrenamiento")
    void confirmarCreaElEntrenamiento() {
        sesionGuardada("2026-06-29", "18:00", "20:00", Sesion.ACEPTADA);

        SesionDTO dto = servicio.confirmar(yo.getEmail(), 10L);

        ArgumentCaptor<Entrenamiento> capturado = ArgumentCaptor.forClass(Entrenamiento.class);
        Mockito.verify(entrenamientoRepository).save(capturado.capture());

        Entrenamiento e = capturado.getValue();
        assertEquals(yo, e.getUsuario());
        assertEquals(LocalDate.of(2026, 6, 29), e.getFecha());
        assertEquals(120, e.getDuracionMinutos());
        assertTrue(e.getTipo().contains("Ana"), "El entrenamiento tiene que decir con quien fue");
        assertTrue(dto.yaConfirmada());
    }

    @Test
    @DisplayName("Cada uno apunta lo suyo: confirmar no entrena por el otro")
    void cadaUnoLoSuyo() {
        Sesion s = sesionGuardada("2026-06-29", "18:00", "20:00", Sesion.ACEPTADA);

        servicio.confirmar(yo.getEmail(), 10L);

        // Que yo diga que fui no prueba que fuera Ana.
        assertTrue(s.isConfirmadaProponente());
        assertFalse(s.isConfirmadaInvitado());
        Mockito.verify(entrenamientoRepository, Mockito.times(1)).save(any(Entrenamiento.class));
    }

    @Test
    @DisplayName("Una sesion que todavia no ha ocurrido no se puede apuntar")
    void nadaPorAdelantado() {
        sesionGuardada("2026-07-03", "18:00", "20:00", Sesion.ACEPTADA);

        assertThrows(IllegalArgumentException.class, () -> servicio.confirmar(yo.getEmail(), 10L));
        Mockito.verify(entrenamientoRepository, Mockito.never()).save(any(Entrenamiento.class));
    }

    @Test
    @DisplayName("No se apunta dos veces el mismo entrenamiento")
    void niDosVeces() {
        sesionGuardada("2026-06-29", "18:00", "20:00", Sesion.ACEPTADA);

        servicio.confirmar(yo.getEmail(), 10L);

        assertThrows(IllegalArgumentException.class, () -> servicio.confirmar(yo.getEmail(), 10L));
        Mockito.verify(entrenamientoRepository, Mockito.times(1)).save(any(Entrenamiento.class));
    }

    // --- La lista del tablero ---

    @Test
    @DisplayName("Una sesion pasada que ya has apuntado deja de aparecer")
    void loApuntadoDesaparece() {
        Sesion pasada = sesionGuardada("2026-06-29", "18:00", "20:00", Sesion.ACEPTADA);
        pasada.setConfirmadaProponente(true);
        Mockito.when(sesionRepository.vivasDe(yo.getId())).thenReturn(List.of(pasada));

        // Sin este filtro el tablero anunciaba en futuro —"entrenas con Ana"— un
        // entrenamiento de la semana pasada que ya estaba en el historial.
        assertTrue(servicio.mias(yo.getEmail()).isEmpty());

        // Para el otro sigue contando: el es quien no la ha apuntado todavia.
        Mockito.when(sesionRepository.vivasDe(otro.getId())).thenReturn(List.of(pasada));
        assertEquals(1, servicio.mias(otro.getEmail()).size());
    }

    @Test
    @DisplayName("Una propuesta a la que se le paso la hora tampoco aparece")
    void laPropuestaCaducadaDesaparece() {
        Sesion caducada = sesionGuardada("2026-06-29", "18:00", "20:00", Sesion.PROPUESTA);
        Mockito.when(sesionRepository.vivasDe(otro.getId())).thenReturn(List.of(caducada));

        assertTrue(servicio.mias(otro.getEmail()).isEmpty(),
                "Ya no se puede aceptar, asi que no tiene sentido seguir ofreciendola");
    }

    @Test
    @DisplayName("Una sesion pasada sin apuntar sigue esperando respuesta")
    void loPasadoSinApuntarSigue() {
        Sesion pasada = sesionGuardada("2026-06-29", "18:00", "20:00", Sesion.ACEPTADA);
        Mockito.when(sesionRepository.vivasDe(yo.getId())).thenReturn(List.of(pasada));

        SesionDTO dto = servicio.mias(yo.getEmail()).get(0);
        assertTrue(dto.puedoConfirmar());
    }

    // --- El contador de la campana ---

    @Test
    @DisplayName("Solo cuentan las propuestas que esperan respuesta tuya")
    void elContadorSoloMiraLoQueTeToca() {
        servicio.pendientesParaMi(yo.getEmail());

        // Como invitado, no como proponente: lo que has propuesto tu no espera
        // nada de ti, asi que anunciarlo en la campana seria mandarte a una
        // pantalla donde no hay nada que hacer.
        Mockito.verify(sesionRepository).contarPendientesDe(
                yo.getId(), AHORA.toLocalDate(), AHORA.toLocalTime());
    }

    @Test
    @DisplayName("El contador devuelve lo que dice la base, sin retoques")
    void elContadorNoInventa() {
        Mockito.when(sesionRepository.contarPendientesDe(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(3L);

        assertEquals(3L, servicio.pendientesParaMi(yo.getEmail()));
    }

    // --- Sugerencia ---

    @Test
    @DisplayName("Sin horarios en comun, el formulario lo dice en vez de inventarse una hora")
    void sugerenciaSinFranjas() {
        SugerenciaSesionDTO s = servicio.sugerir(yo.getEmail(), otro.getId());

        assertFalse(s.hayFranjas());
        assertNull(s.fecha());
    }

    @Test
    @DisplayName("La sugerencia sale del solape real, no de un valor por defecto")
    void sugerenciaConFranjas() {
        Disponibilidad mia = new Disponibilidad("Viernes",
                LocalTime.of(18, 0), LocalTime.of(20, 0), yo, true);
        Disponibilidad suya = new Disponibilidad("Viernes",
                LocalTime.of(19, 0), LocalTime.of(21, 0), otro, true);

        Mockito.when(disponibilidadRepository.findByUsuarioId(yo.getId())).thenReturn(List.of(mia));
        Mockito.when(disponibilidadRepository.findByUsuarioId(otro.getId())).thenReturn(List.of(suya));

        SugerenciaSesionDTO s = servicio.sugerir(yo.getEmail(), otro.getId());

        assertTrue(s.hayFranjas());
        assertEquals(LocalDate.of(2026, 7, 3), s.fecha());
        // El tramo comun es la interseccion, no la franja entera de ninguno.
        assertEquals(LocalTime.of(19, 0), s.horaInicio());
        assertEquals(LocalTime.of(20, 0), s.horaFin());
        assertTrue(s.ambosFijos());
    }

    // ===================== Donde se queda =====================
    // Dos personas de gimnasios distintos pueden entrenar juntas, pero alguien
    // tiene que decir donde. Antes la sesion se guardaba sin sitio: quedaban a
    // una hora y en ninguna parte.

    @Test
    @DisplayName("Con gimnasios distintos se ofrecen los dos para elegir")
    void conGimnasiosDistintosHayQueElegir() {
        yo.setGimnasio(gimnasio(1L, "McFit"));
        otro.setGimnasio(gimnasio(2L, "Basic-Fit"));

        SugerenciaSesionDTO s = servicio.sugerir(yo.getEmail(), otro.getId());

        assertEquals(2, s.donde().size());
        assertEquals("mio", s.donde().get(0).dequien());
        assertEquals("McFit", s.donde().get(0).nombre());
        assertEquals("suyo", s.donde().get(1).dequien());
        assertEquals("Basic-Fit", s.donde().get(1).nombre());
        // Y no se da por hecho ninguno
        assertNull(s.gimnasioNombre());
    }

    @Test
    @DisplayName("Compartiendo gimnasio no se pregunta nada")
    void compartiendoGimnasioNoSePregunta() {
        yo.setGimnasio(gimnasio);
        otro.setGimnasio(gimnasio);

        SugerenciaSesionDTO s = servicio.sugerir(yo.getEmail(), otro.getId());

        assertTrue(s.donde().isEmpty());
        assertEquals("McFit", s.gimnasioNombre());
    }

    @Test
    @DisplayName("Sin gimnasio en alguno de los dos tampoco hay nada que elegir")
    void sinGimnasioNoHayOpciones() {
        yo.setGimnasio(gimnasio);
        otro.setGimnasio(null);

        SugerenciaSesionDTO s = servicio.sugerir(yo.getEmail(), otro.getId());

        assertTrue(s.donde().isEmpty());
    }

    @Test
    @DisplayName("El gimnasio elegido se guarda en la sesion")
    void elGimnasioElegidoSeGuarda() {
        Gimnasio suyo = gimnasio(2L, "Basic-Fit");
        yo.setGimnasio(gimnasio);
        otro.setGimnasio(suyo);
        Mockito.when(gimnasioRepository.findById(2L)).thenReturn(Optional.of(suyo));

        servicio.proponer(yo.getEmail(), otro.getId(), datosEn("2026-07-03", "19:00", "20:30", 2L));

        ArgumentCaptor<Sesion> capturada = ArgumentCaptor.forClass(Sesion.class);
        Mockito.verify(sesionRepository).save(capturada.capture());
        assertEquals("Basic-Fit", capturada.getValue().getGimnasio().getNombre());
    }

    @Test
    @DisplayName("Un gimnasio que no existe se rechaza, no se guarda a medias")
    void gimnasioInventadoSeRechaza() {
        yo.setGimnasio(gimnasio);
        otro.setGimnasio(gimnasio(2L, "Basic-Fit"));
        Mockito.when(gimnasioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                servicio.proponer(yo.getEmail(), otro.getId(),
                        datosEn("2026-07-03", "19:00", "20:30", 99L)));
    }

    @Test
    @DisplayName("Sin elegir y con gimnasios distintos, la sesion sigue sin sitio")
    void sinElegirYSinCompartirNoSeInventaSitio() {
        yo.setGimnasio(gimnasio);
        otro.setGimnasio(gimnasio(2L, "Basic-Fit"));

        servicio.proponer(yo.getEmail(), otro.getId(), datos("2026-07-03", "19:00", "20:30"));

        ArgumentCaptor<Sesion> capturada = ArgumentCaptor.forClass(Sesion.class);
        Mockito.verify(sesionRepository).save(capturada.capture());
        // Ponerle uno por defecto seria decidir por ellos, que es lo que se evita.
        assertNull(capturada.getValue().getGimnasio());
    }
}
