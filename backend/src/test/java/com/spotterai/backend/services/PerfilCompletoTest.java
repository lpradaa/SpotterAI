package com.spotterai.backend.services;

import com.spotterai.backend.semantica.VectorDeBiografia;
import com.spotterai.backend.textos.TextosDePrueba;
import com.spotterai.backend.matching.ExplicadorCompatibilidad;
import com.spotterai.backend.dtos.UsuarioPerfilDTO;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El perfil propio se construye a mano, campo a campo, en un Map.
 *
 * Eso significa que añadir una columna a Usuario no obliga a nada: se compila
 * igual y el campo simplemente no viaja. Ya ha pasado tres veces —biografia,
 * metaSemanal y fotoUrl— y el sintoma siempre es el mismo: se guarda, pero al
 * recargar la pantalla ha desaparecido.
 *
 * Esta prueba es la red para eso.
 */
class PerfilCompletoTest {

    private UsuarioRepository usuarioRepository;
    private DisponibilidadRepository disponibilidadRepository;
    private UsuarioServiceImpl servicio;

    @BeforeEach
    void preparar() {
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        disponibilidadRepository = Mockito.mock(DisponibilidadRepository.class);

        servicio = new UsuarioServiceImpl(
                usuarioRepository,
                Mockito.mock(PasswordEncoder.class),
                Mockito.mock(GimnasioRepository.class),
                Mockito.mock(SolicitudRepository.class),
                disponibilidadRepository,
                new ExplicadorCompatibilidad(TextosDePrueba.nuevo()),
                Mockito.mock(HitoRepository.class),
                Mockito.mock(EntrenamientoRepository.class),
                Mockito.mock(LevantamientoRepository.class),
                Mockito.mock(SesionRepository.class),
                Mockito.mock(BloqueoRepository.class),
                TextosDePrueba.nuevo(),
                new VectorDeBiografia(new com.spotterai.backend.semantica.ServicioDeEmbeddings("")),
                Clock.systemDefaultZone());

        Mockito.when(disponibilidadRepository.findByUsuarioId(Mockito.any())).thenReturn(List.of());
    }

    private void conUsuario(Usuario u) {
        Mockito.when(usuarioRepository.findByEmail("yo@test.com")).thenReturn(Optional.of(u));
    }

    private static Usuario usuarioCompleto() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombre("Luis");
        u.setEmail("yo@test.com");
        u.setNivel("Intermedio");
        u.setObjetivos("Hipertrofia");
        u.setEdad(28);
        u.setAvatar("ascua");
        u.setBiografia("Busco alguien constante");
        u.setMetaSemanal(5);
        u.setFotoUrl("/api/medios/abc.png");
        u.setRutina("TORSO_PIERNA");
        return u;
    }

    @Test
    @DisplayName("El perfil devuelve todos los campos que se pueden editar")
    void devuelveTodoLoEditable() {
        conUsuario(usuarioCompleto());

        Map<String, Object> perfil = servicio.obtenerMiPerfilCompleto("yo@test.com");

        // Si se añade un campo editable y no se mete en el mapa, se guarda pero
        // desaparece al recargar. Esta lista tiene que crecer con el formulario.
        for (String campo : List.of("nombre", "email", "edad", "genero", "peso",
                "nivel", "objetivos", "gimnasioId", "avatar", "fotoUrl",
                "biografia", "metaSemanal", "rutina", "horarios")) {
            assertTrue(perfil.containsKey(campo), "Falta '" + campo + "' en el perfil propio");
        }
    }

    @Test
    @DisplayName("La foto de perfil viaja de vuelta")
    void devuelveLaFoto() {
        conUsuario(usuarioCompleto());

        assertEquals("/api/medios/abc.png",
                servicio.obtenerMiPerfilCompleto("yo@test.com").get("fotoUrl"));
    }

    @Test
    @DisplayName("Sin foto el campo viaja igual, en nulo")
    void sinFotoElCampoSigueEstando() {
        Usuario sinFoto = usuarioCompleto();
        sinFoto.setFotoUrl(null);
        conUsuario(sinFoto);

        Map<String, Object> perfil = servicio.obtenerMiPerfilCompleto("yo@test.com");

        // Que la clave exista importa: el frontend distingue "no tiene foto" de
        // "el backend no me lo ha dicho".
        assertTrue(perfil.containsKey("fotoUrl"));
        assertNull(perfil.get("fotoUrl"));
    }

    @Test
    @DisplayName("La meta semanal cae a 4 si nunca se ha puesto")
    void metaPorDefecto() {
        Usuario sinMeta = usuarioCompleto();
        sinMeta.setMetaSemanal(null);
        conUsuario(sinMeta);

        assertEquals(4, servicio.obtenerMiPerfilCompleto("yo@test.com").get("metaSemanal"));
    }

    @Test
    @DisplayName("Guardar solo una parte no borra el resto")
    void guardarUnaParteNoBorraLoDemas() {
        Usuario u = usuarioCompleto();
        conUsuario(u);
        Mockito.when(usuarioRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        // Lo que manda la pantalla de bienvenida: solo el horario.
        UsuarioPerfilDTO soloHorario = new UsuarioPerfilDTO();
        soloHorario.setHorarios(java.util.List.of());

        servicio.actualizarPerfil("yo@test.com", soloHorario);

        // Antes esto era un reemplazo y dejaba a null todo lo que no viniera. Con
        // el perfil minimo obligatorio deja de ser academico: un onboarding por
        // pasos guardaria el primero y se llevaria por delante el segundo.
        assertEquals("Intermedio", u.getNivel());
        assertEquals("Hipertrofia", u.getObjetivos());
        assertEquals(28, u.getEdad());
        assertEquals("TORSO_PIERNA", u.getRutina());
        assertEquals("Busco alguien constante", u.getBiografia());
    }

    @Test
    @DisplayName("Una cadena vacía sí limpia: es una decisión del formulario")
    void elVacioSiLimpia() {
        Usuario u = usuarioCompleto();
        conUsuario(u);
        Mockito.when(usuarioRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        UsuarioPerfilDTO borrarBio = new UsuarioPerfilDTO();
        borrarBio.setBiografia("");

        servicio.actualizarPerfil("yo@test.com", borrarBio);

        // "" viene de alguien que ha vaciado el campo; null es que ese campo no
        // venia en la peticion. Confundirlos deja sin forma de borrar una
        // biografia.
        assertNull(u.getBiografia());
        assertEquals("Intermedio", u.getNivel());
    }

    @Test
    @DisplayName("El perfil dice qué le falta para poder emparejarse")
    void elPerfilDiceQueLeFalta() {
        conUsuario(usuarioCompleto());

        Map<String, Object> perfil = servicio.obtenerMiPerfilCompleto("yo@test.com");

        assertTrue(perfil.containsKey("perfilMinimo"),
                "El guardián necesita saber si se puede emparejar a esta persona");
    }

    @Test
    @DisplayName("Todo el mundo empieza recibiendo avisos, y el perfil lo dice")
    void elPerfilDiceSiRecibesAvisos() {
        conUsuario(usuarioCompleto());

        assertEquals(true, servicio.obtenerMiPerfilCompleto("yo@test.com").get("avisosPorCorreo"));
    }

    @Test
    @DisplayName("Se puede dejar de recibirlos desde el perfil")
    void sePuedeDarseDeBajaDesdeElPerfil() {
        Usuario u = usuarioCompleto();
        conUsuario(u);
        Mockito.when(usuarioRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        UsuarioPerfilDTO dto = new UsuarioPerfilDTO();
        dto.setAvisosPorCorreo(false);

        servicio.actualizarPerfil("yo@test.com", dto);

        assertFalse(u.isAvisosPorCorreo());
    }

    @Test
    @DisplayName("Un guardado que no la menciona NO da de baja a nadie")
    void unGuardadoParcialNoDaDeBaja() {
        Usuario u = usuarioCompleto();
        conUsuario(u);
        Mockito.when(usuarioRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        // Es la razon de que el campo del DTO sea Boolean y no boolean. Con un
        // primitivo, cualquier guardado parcial que no lo incluyera llegaria
        // como false y daria de baja a quien solo queria cambiar su biografia.
        // Un aviso se deja de recibir porque alguien lo pide, nunca porque pase.
        UsuarioPerfilDTO soloBiografia = new UsuarioPerfilDTO();
        soloBiografia.setBiografia("Busco a alguien constante");

        servicio.actualizarPerfil("yo@test.com", soloBiografia);

        assertTrue(u.isAvisosPorCorreo());
    }
}
