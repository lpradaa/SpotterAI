package com.spotterai.backend.semantica;

import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Calcula los vectores que faltan, una vez, al arrancar.
 *
 * <p>El vector de una biografia se calcula al guardar el perfil. Eso deja fuera
 * a todo el que ya tuviera biografia antes de que existiera el factor: sin este
 * repaso, el factor semantico quedaria "sin datos" para todo el mundo hasta que
 * cada uno volviera a editar su perfil, que es un dia que no llega.
 *
 * <p>Corre <b>despues</b> de que la aplicacion este lista y <b>fuera</b> del
 * hilo de arranque. Trece biografias son trece llamadas a un modelo: si esto
 * bloqueara el arranque, un servicio de embeddings lento retrasaria el
 * despliegue entero, y uno caido lo impediria. Como va aparte, lo peor que pasa
 * es que algunos vectores tarden en aparecer.
 */
@Component
public class RepasoDeVectores {

    private static final Logger log = LoggerFactory.getLogger(RepasoDeVectores.class);

    private final UsuarioRepository usuarios;
    private final VectorDeBiografia vectores;

    public RepasoDeVectores(UsuarioRepository usuarios, VectorDeBiografia vectores) {
        this.usuarios = usuarios;
        this.vectores = vectores;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void alArrancar() {
        List<Usuario> pendientes = usuarios.findAll().stream()
                .filter(u -> !vectores.estaAlDia(u))
                .toList();

        if (pendientes.isEmpty()) return;

        log.info("Repaso de vectores: {} biografías sin vector", pendientes.size());

        int hechos = 0;
        for (Usuario usuario : pendientes) {
            if (vectores.actualizar(usuario)) {
                usuarios.save(usuario);
                hechos++;
            }
        }

        // Se dice cuantos quedaron sin hacer, no solo cuantos salieron: si el
        // servicio de embeddings esta caido, "0 de 13" es la informacion util, y
        // un log que solo cuente exitos no la da.
        log.info("Repaso de vectores: {} de {} calculados", hechos, pendientes.size());
    }
}
