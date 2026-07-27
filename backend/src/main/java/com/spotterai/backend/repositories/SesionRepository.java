package com.spotterai.backend.repositories;

import com.spotterai.backend.models.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SesionRepository extends JpaRepository<Sesion, Long> {

    /**
     * Todo lo que hay entre dos personas, en cualquier direccion.
     *
     * <p>Va en una sola consulta y no en dos porque quien propone y quien es
     * invitado cambia de una sesion a otra: son dos columnas distintas para el
     * mismo par.
     */
    @Query("""
            SELECT s FROM Sesion s
             WHERE (s.proponente.id = :unoId AND s.invitado.id = :otroId)
                OR (s.proponente.id = :otroId AND s.invitado.id = :unoId)
             ORDER BY s.fecha DESC, s.horaInicio DESC
            """)
    List<Sesion> entreLosDos(@Param("unoId") Long unoId, @Param("otroId") Long otroId);

    /** La propuesta viva entre dos personas, si la hay. Solo puede haber una. */
    @Query("""
            SELECT s FROM Sesion s
             WHERE s.estado = 'PROPUESTA'
               AND ((s.proponente.id = :unoId AND s.invitado.id = :otroId)
                 OR (s.proponente.id = :otroId AND s.invitado.id = :unoId))
            """)
    Optional<Sesion> propuestaViva(@Param("unoId") Long unoId, @Param("otroId") Long otroId);

    /**
     * Las sesiones de alguien que siguen contando: pendientes de responder y
     * aceptadas. Las rechazadas y canceladas no se listan porque no hay nada que
     * hacer con ellas.
     */
    @Query("""
            SELECT s FROM Sesion s
             WHERE (s.proponente.id = :usuarioId OR s.invitado.id = :usuarioId)
               AND s.estado IN ('PROPUESTA', 'ACEPTADA')
             ORDER BY s.fecha ASC, s.horaInicio ASC
            """)
    List<Sesion> vivasDe(@Param("usuarioId") Long usuarioId);
}
