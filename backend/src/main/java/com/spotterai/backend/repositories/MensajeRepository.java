package com.spotterai.backend.repositories;

import com.spotterai.backend.dtos.SinLeerPorEmisor;
import com.spotterai.backend.models.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    // Devuelve los mensajes donde el usuario es el receptor, ordenados por fecha
    List<Mensaje> findByReceptorIdOrderByFechaEnvioDesc(Long receptorId);

    // Devuelve los mensajes que un usuario ha enviado
    List<Mensaje> findByEmisorIdOrderByFechaEnvioDesc(Long emisorId);

    @Query("SELECT m FROM Mensaje m WHERE " +
           "(m.emisor.id = :usuario1Id AND m.receptor.id = :usuario2Id) OR " +
           "(m.emisor.id = :usuario2Id AND m.receptor.id = :usuario1Id) " +
           "ORDER BY m.fechaEnvio ASC")
    List<Mensaje> obtenerHistorialChat(@Param("usuario1Id") Long usuario1Id, @Param("usuario2Id") Long usuario2Id);

    /**
     * El ultimo mensaje de cada conversacion del usuario, uno por interlocutor.
     *
     * El GROUP BY va sobre "quien es el otro", de ahi el CASE: una conversacion
     * son los mensajes en los dos sentidos, no una fila con dueño. Y se agrupa
     * por MAX(id) en vez de por MAX(fechaEnvio) porque dos mensajes pueden caer
     * en el mismo instante y entonces saldrian los dos; el id es unico y crece,
     * asi que identifica el ultimo sin ambiguedad.
     *
     * Una sola consulta para toda la lista, en lugar de una por compañero.
     */
    @Query("""
            SELECT m FROM Mensaje m
            WHERE m.id IN (
                SELECT MAX(m2.id) FROM Mensaje m2
                WHERE m2.emisor.id = :usuarioId OR m2.receptor.id = :usuarioId
                GROUP BY CASE WHEN m2.emisor.id = :usuarioId
                              THEN m2.receptor.id ELSE m2.emisor.id END
            )
            ORDER BY m.fechaEnvio DESC
            """)
    List<Mensaje> ultimoMensajePorConversacion(@Param("usuarioId") Long usuarioId);

    /** Cuantos mensajes sin leer tiene el usuario de cada interlocutor. */
    @Query("""
            SELECT new com.spotterai.backend.dtos.SinLeerPorEmisor(m.emisor.id, COUNT(m))
            FROM Mensaje m
            WHERE m.receptor.id = :usuarioId AND m.leido = false
            GROUP BY m.emisor.id
            """)
    List<SinLeerPorEmisor> contarSinLeerPorEmisor(@Param("usuarioId") Long usuarioId);

    /** Total sin leer, para el indicador de la cabecera. */
    long countByReceptorIdAndLeidoFalse(Long receptorId);

    /**
     * Marca como leida una conversacion entera.
     *
     * Va como update masivo y no cargando entidades porque abrir un chat con
     * cien mensajes sin leer no deberia traerse cien filas para tocar un boolean.
     */
    @Modifying
    @Query("UPDATE Mensaje m SET m.leido = true " +
           "WHERE m.receptor.id = :usuarioId AND m.emisor.id = :otroId AND m.leido = false")
    int marcarConversacionLeida(@Param("usuarioId") Long usuarioId, @Param("otroId") Long otroId);

    /**
     * Todo lo que mandaste y todo lo que te mandaron.
     *
     * <p>El hilo entero, no solo tus mensajes: cada uno apunta a las dos
     * personas, asi que los suyos tambien te referencian. Ver {@code
     * BorradoDeCuenta}, donde esta la decision y por que se toma asi.
     */
    @Modifying
    @Query("DELETE FROM Mensaje m WHERE m.emisor.id = :usuarioId OR m.receptor.id = :usuarioId")
    void borrarTodosDe(@Param("usuarioId") Long usuarioId);
}
