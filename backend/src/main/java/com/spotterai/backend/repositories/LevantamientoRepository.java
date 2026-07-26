package com.spotterai.backend.repositories;

import com.spotterai.backend.models.Levantamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LevantamientoRepository extends JpaRepository<Levantamiento, Long> {

    List<Levantamiento> findByUsuarioId(Long usuarioId);

    /**
     * Los de un grupo entero, para puntuar una lista de candidatos sin una
     * consulta por persona.
     */
    List<Levantamiento> findByUsuarioIdIn(List<Long> usuarioIds);

    void deleteByUsuarioId(Long usuarioId);
}
