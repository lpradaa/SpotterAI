package com.spotterai.backend.repositories;

import com.spotterai.backend.models.Entrenamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EntrenamientoRepository extends JpaRepository<Entrenamiento, Long> {
    // Busca los entrenamientos de un usuario y los ordena del más reciente al más antiguo
    List<Entrenamiento> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    /**
     * Cuantas veces ha entrenado alguien desde una fecha.
     *
     * Se enseña en su perfil como señal de que esta activo. En una aplicacion
     * para buscar companero constante, "ha ido cuatro veces esta semana" dice
     * mas que cualquier porcentaje.
     */
    long countByUsuarioIdAndFechaGreaterThanEqual(Long usuarioId, LocalDate desde);

    /** Los entrenamientos recientes de un grupo, en una sola consulta. */
    List<Entrenamiento> findByUsuarioIdInAndFechaGreaterThanEqualOrderByFechaDesc(
            List<Long> usuarioIds, LocalDate desde);
}