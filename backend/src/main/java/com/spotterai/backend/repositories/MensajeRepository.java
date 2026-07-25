package com.spotterai.backend.repositories;

import com.spotterai.backend.models.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
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
}