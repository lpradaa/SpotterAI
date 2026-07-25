package com.spotterai.backend.eventos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Canal de eventos del servidor hacia el navegador (SSE).
 *
 * Se eligió SSE y no WebSocket porque aquí solo hace falta una dirección: enviar
 * un mensaje o responder una solicitud ya son peticiones HTTP normales, lo que
 * faltaba era enterarse de lo que llega. SseEmitter viene en spring-webmvc, así
 * que no añade dependencias, y al ser una petición HTTP corriente no obliga a
 * montar una segunda vía de autenticación.
 *
 * Un usuario puede tener varias conexiones abiertas a la vez (varias pestañas),
 * de ahí que el mapa guarde una lista y no un único emisor.
 */
@Service
public class CanalEventos {

    private static final Logger log = LoggerFactory.getLogger(CanalEventos.class);

    /**
     * Al vencer, el navegador reconecta solo. Es deseable: recicla conexiones
     * que se hayan quedado colgadas sin que nadie tenga que detectarlo.
     */
    static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Map<Long, Collection<SseEmitter>> abiertos = new ConcurrentHashMap<>();

    /** Abre una conexión para un usuario y la deja registrada. */
    public SseEmitter abrir(Long usuarioId) {
        SseEmitter emisor = new SseEmitter(TIMEOUT_MS);

        // Los tres se limpian igual: si la conexión ya no sirve, fuera del mapa.
        // Sin esto el mapa crece con cada recarga de página hasta agotar memoria.
        emisor.onCompletion(() -> retirar(usuarioId, emisor));
        emisor.onTimeout(() -> retirar(usuarioId, emisor));
        emisor.onError(e -> retirar(usuarioId, emisor));

        abiertos.computeIfAbsent(usuarioId, id -> new CopyOnWriteArrayList<>()).add(emisor);

        // Un primer evento inmediato: hasta que no llega algo, el navegador no
        // considera establecida la conexión y la interfaz no sabe si está viva.
        enviar(usuarioId, emisor, "conectado", Map.of("usuarioId", usuarioId));

        return emisor;
    }

    /**
     * Publica un evento a todas las conexiones abiertas de un usuario.
     *
     * Si no tiene ninguna no pasa nada: no está mirando, y al entrar cargará el
     * estado con las peticiones normales. Los eventos no sustituyen a la carga
     * inicial, solo evitan tener que recargar.
     */
    public void publicar(Long usuarioId, TipoEvento tipo, Object datos) {
        Collection<SseEmitter> emisores = abiertos.get(usuarioId);
        if (emisores == null) return;

        for (SseEmitter emisor : emisores) {
            enviar(usuarioId, emisor, tipo.getClave(), datos);
        }
    }

    /** Conexiones vivas de un usuario. Solo para pruebas y diagnóstico. */
    public int conexionesDe(Long usuarioId) {
        Collection<SseEmitter> emisores = abiertos.get(usuarioId);
        return emisores == null ? 0 : emisores.size();
    }

    /**
     * Comentario periódico para que proxies y cortafuegos no den la conexión
     * por muerta. Un comentario SSE no dispara ningún manejador en el cliente.
     */
    @Scheduled(fixedDelay = 20_000)
    public void latido() {
        abiertos.forEach((usuarioId, emisores) -> {
            for (SseEmitter emisor : emisores) {
                try {
                    emisor.send(SseEmitter.event().comment("latido"));
                } catch (IOException | IllegalStateException e) {
                    // El cliente cerró sin avisar. No es un error que reportar.
                    retirar(usuarioId, emisor);
                }
            }
        });
    }

    private void enviar(Long usuarioId, SseEmitter emisor, String tipo, Object datos) {
        try {
            emisor.send(SseEmitter.event().name(tipo).data(datos));
        } catch (IOException | IllegalStateException e) {
            log.debug("Conexión de eventos caída para el usuario {}: {}", usuarioId, e.getMessage());
            retirar(usuarioId, emisor);
        }
    }

    /**
     * Quita un emisor y, si era el último del usuario, borra también la entrada.
     *
     * Va dentro de computeIfPresent para que no se pueda dejar la lista vacía en
     * el mapa entre el remove y la comprobación, ni borrar una lista en la que
     * otro hilo acaba de registrar una conexión nueva.
     */
    void retirar(Long usuarioId, SseEmitter emisor) {
        abiertos.computeIfPresent(usuarioId, (id, emisores) -> {
            emisores.remove(emisor);
            return emisores.isEmpty() ? null : emisores;
        });
        log.debug("Conexiones de eventos del usuario {}: {}", usuarioId, conexionesDe(usuarioId));
    }
}
