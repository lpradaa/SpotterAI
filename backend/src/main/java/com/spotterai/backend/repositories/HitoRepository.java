package com.spotterai.backend.repositories;

import com.spotterai.backend.dtos.HitosPorUsuario;
import com.spotterai.backend.models.Hito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HitoRepository extends JpaRepository<Hito, Long> {

    /** Los de una persona, del mas reciente al mas antiguo. */
    List<Hito> findByUsuarioIdOrderByFechaDescIdDesc(Long usuarioId);

    /**
     * Cuantos hitos tiene cada uno de un grupo.
     *
     * Para poder enseñar el dato en una lista de tarjetas sin una consulta por
     * persona.
     */
    @Query("""
            SELECT new com.spotterai.backend.dtos.HitosPorUsuario(h.usuario.id, COUNT(h))
            FROM Hito h
            WHERE h.usuario.id IN :ids
            GROUP BY h.usuario.id
            """)
    List<HitosPorUsuario> contarPorUsuario(@Param("ids") List<Long> ids);

    /**
     * Los hitos recientes de un grupo de personas.
     *
     * <p>Una consulta para todo el grupo y no una por cabeza: la actividad del
     * tablero se pide en cada carga, y con una consulta por companero seria el
     * clasico N+1 en la pantalla que mas se visita.
     */
    List<Hito> findByUsuarioIdInOrderByFechaDescIdDesc(List<Long> usuarioIds);

    @Modifying
    @Query("DELETE FROM Hito h WHERE h.usuario.id = :usuarioId")
    void borrarTodosDe(@Param("usuarioId") Long usuarioId);
}
