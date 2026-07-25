package com.spotterai.backend.eventos;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lo que se prueba aquí es la contabilidad de conexiones, que es donde están las
 * fugas: un mapa de emisores que no se limpia crece con cada recarga de página.
 */
class CanalEventosTest {

    @Test
    void alAbrirQuedaRegistradaLaConexión() {
        CanalEventos canal = new CanalEventos();

        canal.abrir(1L);

        assertEquals(1, canal.conexionesDe(1L));
    }

    @Test
    void unUsuarioPuedeTenerVariasPestañasAbiertas() {
        CanalEventos canal = new CanalEventos();

        canal.abrir(1L);
        canal.abrir(1L);

        assertEquals(2, canal.conexionesDe(1L),
                "Si el mapa guardara un solo emisor, abrir una pestaña dejaría muda a la otra");
    }

    /*
     * Estas dos llaman a retirar() directamente en vez de a emisor.complete().
     *
     * complete() no dispara onCompletion fuera de un contenedor: Spring registra
     * esos callbacks en el manejador de la petición asíncrona y es el contenedor
     * quien los invoca al cerrarla. Lo que sí se puede comprobar aquí, que es
     * donde está la lógica delicada, es la contabilidad del mapa. Que el cable
     * entre el cierre real y retirar() esté puesto se ve en el log al cerrar una
     * pestaña con el nivel debug de CanalEventos.
     */

    @Test
    void alRetirarLaÚnicaConexiónDesapareceLaEntradaDelUsuario() {
        CanalEventos canal = new CanalEventos();
        SseEmitter emisor = canal.abrir(1L);

        canal.retirar(1L, emisor);

        assertEquals(0, canal.conexionesDe(1L));
    }

    @Test
    void cerrarUnaPestañaNoAfectaALaOtra() {
        CanalEventos canal = new CanalEventos();
        SseEmitter primera = canal.abrir(1L);
        canal.abrir(1L);

        canal.retirar(1L, primera);

        assertEquals(1, canal.conexionesDe(1L));
    }

    @Test
    void retirarDosVecesLaMismaConexiónNoRompe() {
        CanalEventos canal = new CanalEventos();
        SseEmitter emisor = canal.abrir(1L);

        // Pasa de verdad: onError y onCompletion pueden dispararse los dos.
        canal.retirar(1L, emisor);

        assertDoesNotThrow(() -> canal.retirar(1L, emisor));
        assertEquals(0, canal.conexionesDe(1L));
    }

    @Test
    void publicarAQuienNoEstáConectadoNoRompe() {
        CanalEventos canal = new CanalEventos();

        // Caso corriente: se envía un mensaje a alguien que no tiene la app abierta.
        // Lo verá al entrar, con la carga normal; el evento simplemente se pierde.
        assertDoesNotThrow(() -> canal.publicar(99L, TipoEvento.MENSAJE, Map.of("texto", "hola")));
    }

    @Test
    void elLatidoNoRompeSiNoHayNadieConectado() {
        CanalEventos canal = new CanalEventos();

        assertDoesNotThrow(canal::latido);
    }

    @Test
    void unEventoNoLlegaAOtroUsuario() {
        CanalEventos canal = new CanalEventos();
        canal.abrir(1L);

        canal.publicar(2L, TipoEvento.MENSAJE, Map.of("texto", "privado"));

        assertEquals(0, canal.conexionesDe(2L));
        assertEquals(1, canal.conexionesDe(1L));
    }
}
