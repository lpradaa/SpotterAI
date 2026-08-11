package com.spotterai.backend.services;

import com.spotterai.backend.models.Gimnasio;
import java.util.List;

public interface GimnasioService {
    List<Gimnasio> obtenerTodos();
    List<Gimnasio> buscarPorCiudad(String ciudad);
    Gimnasio guardar(Gimnasio gimnasio);

    /**
     * El gimnasio que se llama asi, creandolo si no habia ninguno.
     *
     * <p>Comparando por {@link NombreDeGimnasio#normalizar}, para que escribirlo
     * con otra caja o con una tilde distinta no parta en dos a la gente que
     * entrena en el mismo sitio.
     *
     * @throws IllegalArgumentException si el nombre viene vacio
     */
    Gimnasio buscarOCrear(String nombre);
}