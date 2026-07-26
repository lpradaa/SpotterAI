package com.spotterai.backend.services;

import com.spotterai.backend.matching.ExplicadorCompatibilidad;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

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
                new ExplicadorCompatibilidad(),
                Mockito.mock(HitoRepository.class),
                Mockito.mock(EntrenamientoRepository.class));

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
                "biografia", "metaSemanal", "horarios")) {
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
}
