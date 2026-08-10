package com.spotterai.backend.repositories;

import com.spotterai.backend.dtos.ConteoPorUsuario;
import com.spotterai.backend.models.Entrenamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Cuantos ha registrado cada uno desde una fecha, y cuantos en total.
     *
     * <p>Dos consultas para todo el grupo, no dos por persona: la constancia
     * entra en la puntuacion de cada candidato, asi que preguntarlo de uno en
     * uno seria el N+1 clasico en la consulta mas cara de la aplicacion.
     *
     * <p>El total hace falta aparte del reciente para distinguir a quien acaba
     * de entrar de quien lo dejo: los dos tienen cero entrenamientos este mes, y
     * no significan lo mismo.
     */
    @Query("""
            SELECT new com.spotterai.backend.dtos.ConteoPorUsuario(e.usuario.id, COUNT(e))
            FROM Entrenamiento e
            WHERE e.usuario.id IN :ids AND e.fecha >= :desde
            GROUP BY e.usuario.id
            """)
    List<ConteoPorUsuario> contarDesde(@Param("ids") List<Long> ids,
                                       @Param("desde") LocalDate desde);

    @Query("""
            SELECT new com.spotterai.backend.dtos.ConteoPorUsuario(e.usuario.id, COUNT(e))
            FROM Entrenamiento e
            WHERE e.usuario.id IN :ids
            GROUP BY e.usuario.id
            """)
    List<ConteoPorUsuario> contarTotales(@Param("ids") List<Long> ids);

    /** Los entrenamientos recientes de un grupo, en una sola consulta. */
    List<Entrenamiento> findByUsuarioIdInAndFechaGreaterThanEqualOrderByFechaDesc(
            List<Long> usuarioIds, LocalDate desde);

    @Modifying
    @Query("DELETE FROM Entrenamiento e WHERE e.usuario.id = :usuarioId")
    void borrarTodosDe(@Param("usuarioId") Long usuarioId);
}
