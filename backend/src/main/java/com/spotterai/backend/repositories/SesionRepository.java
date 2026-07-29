package com.spotterai.backend.repositories;

import com.spotterai.backend.metricas.ParDeUsuarios;
import com.spotterai.backend.models.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    /**
     * Propuestas que esperan tu respuesta.
     *
     * <p>Solo como invitado: las que has hecho tú no esperan nada de ti. Y solo
     * las que todavia se pueden aceptar, porque anunciar en la campana algo que
     * ya no se puede contestar es mandar a alguien a una pantalla donde no hay
     * nada que hacer.
     */
    @Query("""
            SELECT COUNT(s) FROM Sesion s
             WHERE s.invitado.id = :usuarioId
               AND s.estado = 'PROPUESTA'
               AND (s.fecha > :hoy OR (s.fecha = :hoy AND s.horaInicio > :ahora))
            """)
    long contarPendientesDe(@Param("usuarioId") Long usuarioId,
                            @Param("hoy") LocalDate hoy,
                            @Param("ahora") LocalTime ahora);

    /**
     * Cuantas veces han quedado dos personas y esa fecha ya ha pasado.
     *
     * <p>Es lo mas cerca que se puede estar de "cuantas veces habeis entrenado
     * juntos" sin inventar nada: consta que lo acordasteis y consta que el dia
     * llego. Que ademas fuerais es cosa vuestra, y por eso la frase que lo
     * acompaña dice "habeis quedado" y no "habeis entrenado".
     */
    @Query("""
            SELECT COUNT(s) FROM Sesion s
             WHERE s.estado = 'ACEPTADA'
               AND (s.fecha < :hoy OR (s.fecha = :hoy AND s.horaInicio <= :ahora))
               AND ((s.proponente.id = :unoId AND s.invitado.id = :otroId)
                 OR (s.proponente.id = :otroId AND s.invitado.id = :unoId))
            """)
    long contarQuedadasEntre(@Param("unoId") Long unoId,
                             @Param("otroId") Long otroId,
                             @Param("hoy") LocalDate hoy,
                             @Param("ahora") LocalTime ahora);

    /**
     * Las parejas de las que consta que entrenaron juntas de verdad.
     *
     * <p>Las dos confirmaciones y no una: "entrenamos" dicho por los dos es el
     * unico dato de esta aplicacion que no es una intencion. Que quedarais es
     * un acuerdo, que la fecha pasara es un calendario; esto es lo unico que
     * dice que la aplicacion sirvio para lo que existe.
     *
     * <p>Por eso es la ultima columna del embudo, y por eso no vale
     * {@code contarQuedadasEntre}, que a proposito solo llega a "habeis
     * quedado".
     */
    @Query("""
            SELECT DISTINCT new com.spotterai.backend.metricas.ParDeUsuarios(
                       s.proponente.id, s.invitado.id)
              FROM Sesion s
             WHERE s.confirmadaProponente = true
               AND s.confirmadaInvitado = true
            """)
    List<ParDeUsuarios> paresQueEntrenaron();

    /**
     * Las propuestas que siguen sin respuesta y aun no se han avisado por correo.
     *
     * <p>Solo las de un dia que todavia no ha pasado: avisar de una propuesta
     * para el martes cuando ya es miercoles no le sirve a nadie, y encima
     * invita a responder a algo que ya no se puede responder.
     *
     * @param desde limite de antiguedad; mas viejas que esto ya no se avisan
     * @param hasta hay que esperar a que sean al menos asi de viejas
     * @param hoy   para descartar las que ya han pasado
     */
    @Query("""
            SELECT s FROM Sesion s
             WHERE s.estado = 'PROPUESTA'
               AND s.avisadoEn IS NULL
               AND s.creadaEn BETWEEN :desde AND :hasta
               AND s.fecha >= :hoy
               AND s.invitado.avisosPorCorreo = true
            """)
    List<Sesion> propuestasPorAvisar(@Param("desde") LocalDateTime desde,
                                     @Param("hasta") LocalDateTime hasta,
                                     @Param("hoy") LocalDate hoy);
}
