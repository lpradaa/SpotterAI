package com.spotterai.backend.services;

import com.spotterai.backend.models.Gimnasio;
import java.util.List;

public interface GimnasioService {
    List<Gimnasio> obtenerTodos();
    List<Gimnasio> buscarPorCiudad(String ciudad);
    Gimnasio guardar(Gimnasio gimnasio);
}