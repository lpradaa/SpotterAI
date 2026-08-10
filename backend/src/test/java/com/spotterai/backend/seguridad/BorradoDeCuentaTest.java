package com.spotterai.backend.seguridad;

import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Irse de verdad.
 *
 * <p>Un borrado a medias es peor que no borrar: deja a alguien sin cuenta y con
 * sus mensajes en la base. Lo que se fija aquí es que no quede nada suyo, que no
 * pueda hacerlo quien no sabe la contraseña, y el orden — las claves ajenas
 * están en RESTRICT a propósito, así que si algo se borra antes de tiempo el
 * borrado falla entero en vez de dejar basura.
 */
class BorradoDeCuentaTest {

    private UsuarioRepository usuarios;
    private DisponibilidadRepository disponibilidades;
    private EntrenamientoRepository entrenamientos;
    private HitoRepository hitos;
    private LevantamientoRepository levantamientos;
    private MensajeRepository mensajes;
    private SesionRepository sesiones;
    private SolicitudRepository solicitudes;
    private BloqueoRepository bloqueos;

    private BorradoDeCuenta borrado;
    private final PasswordEncoder cifrador = new BCryptPasswordEncoder();
    private final Usuario luis = new Usuario();

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        disponibilidades = mock(DisponibilidadRepository.class);
        entrenamientos = mock(EntrenamientoRepository.class);
        hitos = mock(HitoRepository.class);
        levantamientos = mock(LevantamientoRepository.class);
        mensajes = mock(MensajeRepository.class);
        sesiones = mock(SesionRepository.class);
        solicitudes = mock(SolicitudRepository.class);
        bloqueos = mock(BloqueoRepository.class);

        borrado = new BorradoDeCuenta(usuarios, cifrador, disponibilidades, entrenamientos,
                hitos, levantamientos, mensajes, sesiones, solicitudes, bloqueos);

        luis.setId(7L);
        luis.setEmail("luis@test.com");
        luis.setPassword(cifrador.encode("la-de-siempre-123"));

        when(usuarios.findByEmail("luis@test.com")).thenReturn(Optional.of(luis));
        when(usuarios.findByEmail("nadie@test.com")).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("No queda nada suyo en ninguna tabla")
    void noQuedaNada() {
        assertTrue(borrado.borrar("luis@test.com", "la-de-siempre-123"));

        // Si aparece una tabla nueva que referencie al usuario y no se añada al
        // servicio, el borrado fallará por la clave ajena en vez de dejar
        // basura. Esta lista es la que hay que mantener al día.
        verify(bloqueos).borrarTodosDe(7L);
        verify(mensajes).borrarTodosDe(7L);
        verify(sesiones).borrarTodasDe(7L);
        verify(solicitudes).borrarTodasDe(7L);
        verify(levantamientos).borrarTodosDe(7L);
        verify(entrenamientos).borrarTodosDe(7L);
        verify(hitos).borrarTodosDe(7L);
        verify(disponibilidades).borrarTodasDe(7L);
        verify(usuarios).delete(luis);
    }

    @Test
    @DisplayName("El usuario se borra el último, después de lo que le apunta")
    void elUsuarioVaElUltimo() {
        borrado.borrar("luis@test.com", "la-de-siempre-123");

        // Las claves ajenas están en RESTRICT: borrarlo antes revienta.
        InOrder orden = inOrder(mensajes, sesiones, solicitudes, disponibilidades, usuarios);
        orden.verify(mensajes).borrarTodosDe(7L);
        orden.verify(sesiones).borrarTodasDe(7L);
        orden.verify(solicitudes).borrarTodasDe(7L);
        orden.verify(disponibilidades).borrarTodasDe(7L);
        orden.verify(usuarios).delete(luis);
    }

    @Test
    @DisplayName("Sin la contraseña correcta no se borra nada")
    void sinLaContrasenaNoSeBorraNada() {
        assertFalse(borrado.borrar("luis@test.com", "la-que-no-es"));

        // Una sesión abierta en un ordenador prestado no puede bastar para
        // borrarle la cuenta a alguien, y esto no tiene vuelta atrás.
        verify(usuarios, never()).delete(Mockito.any());
        verify(mensajes, never()).borrarTodosDe(Mockito.any());
    }

    @Test
    @DisplayName("De una cuenta que no existe no se borra nada")
    void deUnaCuentaQueNoExisteNoSeBorraNada() {
        assertFalse(borrado.borrar("nadie@test.com", "la-que-sea"));

        verify(usuarios, never()).delete(Mockito.any());
    }
}
