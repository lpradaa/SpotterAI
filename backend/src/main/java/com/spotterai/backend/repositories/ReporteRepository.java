package com.spotterai.backend.repositories;

import com.spotterai.backend.models.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    /** Para el panel de moderación: los más recientes primero. */
    List<Reporte> findAllByOrderByCreadoEnDesc();

    /** Para poder borrarse la cuenta: los que hiciste y los que te hicieron. */
    @Modifying
    @Query("DELETE FROM Reporte r WHERE r.reportador.id = :usuarioId OR r.reportado.id = :usuarioId")
    void borrarTodosDe(@Param("usuarioId") Long usuarioId);
}
