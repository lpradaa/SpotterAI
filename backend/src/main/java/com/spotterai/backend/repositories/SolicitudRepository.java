package com.spotterai.backend.repositories;

import com.spotterai.backend.models.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    
    // 🔥 CORREGIDO: findFirstBy... evita el error 500 si hay solicitudes duplicadas por accidente
    Optional<Solicitud> findFirstByEmisorIdAndReceptorId(Long emisorId, Long receptorId);

    // Busca todas las solicitudes que ha recibido un usuario y que están en un estado concreto (ej: "PENDIENTE")
    List<Solicitud> findByReceptorIdAndEstado(Long receptorId, String estado);
    
    // Busca todas las solicitudes aceptadas de un usuario (para saber quiénes son sus "Matches")
    List<Solicitud> findByEmisorIdAndEstadoOrReceptorIdAndEstado(Long emisorId, String estado1, Long receptorId, String estado2);

    // Búsqueda de solicitudes aceptadas por usuario
    @Query("SELECT s FROM Solicitud s WHERE (s.emisor.id = :usuarioId OR s.receptor.id = :usuarioId) AND s.estado = 'ACEPTADA'")
    List<Solicitud> findAceptadasPorUsuario(@Param("usuarioId") Long usuarioId);

    // Todas las solicitudes en las que participa el usuario, en cualquier estado y
    // direccion. Se carga una vez y se indexa en memoria para saber el estado de
    // cada candidato sin lanzar dos consultas por candidato.
    @Query("SELECT s FROM Solicitud s WHERE s.emisor.id = :usuarioId OR s.receptor.id = :usuarioId")
    List<Solicitud> findTodasPorUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * Las que llevan la puntuacion congelada, que son las unicas medibles.
     *
     * <p>Las anteriores a que se guardara quedan fuera: no se sabe con que
     * numero se mandaron y rellenarlo con el de hoy seria inventarlo. El embudo
     * cuenta cuantas descarto por esto en vez de callarselo.
     */
    List<Solicitud> findByCompatibilidadIsNotNull();

    long countByCompatibilidadIsNull();
}