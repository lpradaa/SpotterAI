package com.spotterai.backend.repositories;

import com.spotterai.backend.models.Solicitud;
import com.spotterai.backend.dtos.EstadoConCompanero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
     * Lo mismo, pero solo con quien y en que estado.
     *
     * <p>El emparejamiento necesita exactamente eso de cada candidato, y traer
     * las {@code Solicitud} enteras para leerlo salia caro sin que se notara:
     * emisor y receptor son {@code @ManyToOne} —EAGER por defecto— asi que
     * Hibernate materializaba los dos usuarios de cada solicitud, y con ellos su
     * gimnasio, para acabar usando un identificador y una cadena.
     *
     * <p>No parecia grave con los datos de demostracion porque casi nadie tiene
     * solicitudes. Crece justo con lo que se quiere que crezca: cuanta mas gente
     * conectada, mas caro salia calcular la lista.
     *
     * <p>El CASE resuelve en SQL lo que antes se hacia en Java —quien es "el
     * otro" segun quien mandara la solicitud— y {@code s.emisor.id} no obliga a
     * unir con usuario: sale de la clave ajena que ya esta en la fila.
     */
    @Query("""
            SELECT new com.spotterai.backend.dtos.EstadoConCompanero(
                CASE WHEN s.emisor.id = :usuarioId THEN s.receptor.id ELSE s.emisor.id END,
                s.estado)
            FROM Solicitud s
            WHERE s.emisor.id = :usuarioId OR s.receptor.id = :usuarioId
            """)
    List<EstadoConCompanero> estadosPorCompanero(@Param("usuarioId") Long usuarioId);

    /**
     * Las que llevan la puntuacion congelada, que son las unicas medibles.
     *
     * <p>Las anteriores a que se guardara quedan fuera: no se sabe con que
     * numero se mandaron y rellenarlo con el de hoy seria inventarlo. El embudo
     * cuenta cuantas descarto por esto en vez de callarselo.
     */
    List<Solicitud> findByCompatibilidadIsNotNull();

    /**
     * Las que siguen esperando respuesta y todavia no se han avisado por correo.
     *
     * <p>Que siga en PENDIENTE es lo que hace que el aviso valga la pena: quien
     * la vio en directo y contesto ya no aparece aqui, asi que no recibe un
     * correo por algo que ya ha resuelto.
     *
     * @param desde limite de antiguedad; mas viejas que esto ya no se avisan
     * @param hasta hay que esperar a que sean al menos asi de viejas
     */
    @Query("""
            SELECT s FROM Solicitud s
             WHERE s.estado = 'PENDIENTE'
               AND s.avisadoEn IS NULL
               AND s.fechaSolicitud BETWEEN :desde AND :hasta
               AND s.receptor.avisosPorCorreo = true
            """)
    List<Solicitud> pendientesPorAvisar(@Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta);

    long countByCompatibilidadIsNull();

    /** Las solicitudes en las que participabas, en cualquier direccion. */
    @Modifying
    @Query("DELETE FROM Solicitud s WHERE s.emisor.id = :usuarioId OR s.receptor.id = :usuarioId")
    void borrarTodasDe(@Param("usuarioId") Long usuarioId);
}
