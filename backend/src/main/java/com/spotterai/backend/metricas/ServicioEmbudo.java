package com.spotterai.backend.metricas;

import com.spotterai.backend.matching.Tramo;
import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.repositories.SesionRepository;
import com.spotterai.backend.repositories.SolicitudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Arma el embudo cruzando lo que se pidio con lo que acabo pasando.
 *
 * <p>Se calcula al vuelo y no se guarda agregado en ningun sitio. Son dos
 * consultas sobre tablas que en esta aplicacion tienen cientos de filas, no
 * millones, y un agregado guardado es una copia que se queda vieja: la gracia
 * de esto es justamente poder mirarlo despues de tocar los pesos.
 */
@Service
public class ServicioEmbudo {

    private final SolicitudRepository solicitudes;
    private final SesionRepository sesiones;

    public ServicioEmbudo(SolicitudRepository solicitudes, SesionRepository sesiones) {
        this.solicitudes = solicitudes;
        this.sesiones = sesiones;
    }

    @Transactional(readOnly = true)
    public EmbudoDeCompatibilidad calcular() {
        List<Solicitud> medibles = solicitudes.findByCompatibilidadIsNotNull();

        Set<ParDeUsuarios> entrenaron = new HashSet<>(sesiones.paresQueEntrenaron());

        Map<Tramo, List<Solicitud>> porTramo = medibles.stream()
                .collect(Collectors.groupingBy(s -> Tramo.de(s.getCompatibilidad())));

        List<EmbudoDeCompatibilidad.Fila> filas = new ArrayList<>();
        // Sobre los valores del enum y no sobre las claves del mapa: un tramo
        // sin ninguna solicitud es un dato —"nadie ha escrito nunca a alguien de
        // compatibilidad baja"— y desaparecer de la tabla lo esconde.
        for (Tramo tramo : Tramo.values()) {
            List<Solicitud> delTramo = porTramo.getOrDefault(tramo, List.of());

            long aceptadas = delTramo.stream()
                    .filter(s -> "ACEPTADA".equals(s.getEstado()))
                    .count();

            long entrenadas = delTramo.stream()
                    .filter(s -> entrenaron.contains(
                            new ParDeUsuarios(s.getEmisor().getId(), s.getReceptor().getId())))
                    .count();

            filas.add(new EmbudoDeCompatibilidad.Fila(tramo, delTramo.size(), aceptadas, entrenadas));
        }

        return new EmbudoDeCompatibilidad(
                List.copyOf(filas),
                Math.toIntExact(solicitudes.countByCompatibilidadIsNull()));
    }
}
