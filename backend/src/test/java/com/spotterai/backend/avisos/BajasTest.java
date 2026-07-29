package com.spotterai.backend.avisos;

import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Poder salir.
 *
 * <p>Es lo que separa un aviso de un correo no deseado, y no es una opinión: sin
 * salida, lo que hace la gente es marcar el remitente como spam, y a partir de
 * ahí tampoco le llegan los avisos que sí quería. La salida protege a los avisos
 * que importan.
 */
class BajasTest {

    private UsuarioRepository usuarios;
    private Bajas bajas;

    private final Usuario luis = new Usuario();

    @BeforeEach
    void preparar() {
        usuarios = Mockito.mock(UsuarioRepository.class);
        bajas = new Bajas(usuarios);

        luis.setId(1L);
        luis.setEmail("luis@test.com");

        when(usuarios.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarios.findByTokenAvisos(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("La llave se crea la primera vez y se conserva después")
    void laLlaveNoCambia() {
        String primera = bajas.llaveDe(luis);
        String segunda = bajas.llaveDe(luis);

        // Si se generase una nueva en cada correo, el enlace de uno de hace dos
        // semanas dejaria de funcionar, y ese es justo el correo que alguien
        // abre cuando se harta.
        assertEquals(primera, segunda);
        assertEquals(primera, luis.getTokenAvisos());
    }

    @Test
    @DisplayName("Dos personas no comparten llave")
    void cadaUnoLaSuya() {
        Usuario otro = new Usuario();
        otro.setId(2L);

        assertNotEquals(bajas.llaveDe(luis), bajas.llaveDe(otro));
    }

    @Test
    @DisplayName("La llave es aleatoria, no una función del usuario")
    void laLlaveNoSeAdivina() {
        // Dos personas con los mismos datos y el mismo id sacan llaves
        // distintas: es lo que demuestra que no se deriva de nada del usuario.
        // Si se derivara —un id cifrado, un hash del correo—, cualquiera podria
        // dar de baja a cualquiera calculandola.
        Usuario uno = new Usuario();
        uno.setId(1L);
        uno.setEmail("luis@test.com");
        Usuario copia = new Usuario();
        copia.setId(1L);
        copia.setEmail("luis@test.com");

        assertNotEquals(bajas.llaveDe(uno), bajas.llaveDe(copia));
    }

    @Test
    @DisplayName("Es lo bastante larga y cabe en un enlace sin escapar nada")
    void laLlaveEsUsable() {
        String llave = bajas.llaveDe(luis);

        assertTrue(llave.length() >= 40, "Demasiado corta para no poder probarse: " + llave);
        assertTrue(llave.matches("[A-Za-z0-9_-]+"), llave);
    }

    @Test
    @DisplayName("Con la llave buena se deja de recibir")
    void laBajaFunciona() {
        String llave = bajas.llaveDe(luis);
        when(usuarios.findByTokenAvisos(llave)).thenReturn(Optional.of(luis));

        assertTrue(bajas.cambiar(llave, false));
        assertFalse(luis.isAvisosPorCorreo());
    }

    @Test
    @DisplayName("Y se puede volver: un clic mal dado no es definitivo")
    void sePuedeVolver() {
        String llave = bajas.llaveDe(luis);
        when(usuarios.findByTokenAvisos(llave)).thenReturn(Optional.of(luis));

        bajas.cambiar(llave, false);
        assertTrue(bajas.cambiar(llave, true));
        assertTrue(luis.isAvisosPorCorreo());
    }

    @Test
    @DisplayName("Una llave inventada no cambia nada de nadie")
    void unaLlaveFalsaNoHaceNada() {
        assertFalse(bajas.cambiar("me-la-invento", false));
    }

    @Test
    @DisplayName("Ni vacía ni nula: no se busca en la base con eso")
    void niVaciaNiNula() {
        // Sin este corte, un token vacio consultaria la base y encontraria a
        // cualquiera que todavia no tenga llave, o sea a todo el mundo.
        assertFalse(bajas.cambiar(null, false));
        assertFalse(bajas.cambiar("", false));
        assertFalse(bajas.cambiar("   ", false));
        Mockito.verify(usuarios, Mockito.never()).findByTokenAvisos(any());
    }

    @Test
    @DisplayName("Todo el mundo empieza recibiendo avisos")
    void porDefectoSeRecibe() {
        // Quien se registra en una aplicacion para emparejarse con gente espera
        // que le avisen cuando alguien le escribe. Lo que no puede faltar es la
        // salida, no el aviso.
        assertTrue(new Usuario().isAvisosPorCorreo());
    }
}
