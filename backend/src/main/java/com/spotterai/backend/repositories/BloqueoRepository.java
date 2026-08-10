package com.spotterai.backend.repositories;

import com.spotterai.backend.models.Bloqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {

    /**
     * Con quien hay bloqueo, en cualquiera de los dos sentidos.
     *
     * <p>Devuelve ids y no entidades porque quien lo usa lo unico que hace es
     * descartar: la lista de gente cruza esto contra los candidatos y el que
     * aparezca aqui no se enseña.
     *
     * <p>Los dos sentidos importan. Si solo mirara a quien has bloqueado tu,
     * quien te bloqueo a ti te seguiria viendo en su Explorar y podria mandarte
     * una solicitud, que es exactamente lo que venia a impedir.
     */
    @Query("""
            SELECT CASE WHEN b.bloqueador.id = :usuarioId THEN b.bloqueado.id ELSE b.bloqueador.id END
              FROM Bloqueo b
             WHERE b.bloqueador.id = :usuarioId OR b.bloqueado.id = :usuarioId
            """)
    List<Long> idsConBloqueoDe(@Param("usuarioId") Long usuarioId);

    /** Si hay bloqueo entre estos dos, lo pusiera quien lo pusiera. */
    @Query("""
            SELECT COUNT(b) > 0 FROM Bloqueo b
             WHERE (b.bloqueador.id = :unoId AND b.bloqueado.id = :otroId)
                OR (b.bloqueador.id = :otroId AND b.bloqueado.id = :unoId)
            """)
    boolean hayBloqueoEntre(@Param("unoId") Long unoId, @Param("otroId") Long otroId);

    /** Los que has puesto tú, que son los únicos que puedes quitar. */
    List<Bloqueo> findByBloqueadorIdOrderByCreadoEnDesc(Long bloqueadorId);

    Optional<Bloqueo> findByBloqueadorIdAndBloqueadoId(Long bloqueadorId, Long bloqueadoId);

    /** Para poder borrarse la cuenta: los bloqueos son suyos y de quien le bloqueó. */
    @Modifying
    @Query("DELETE FROM Bloqueo b WHERE b.bloqueador.id = :usuarioId OR b.bloqueado.id = :usuarioId")
    void borrarTodosDe(@Param("usuarioId") Long usuarioId);
}
