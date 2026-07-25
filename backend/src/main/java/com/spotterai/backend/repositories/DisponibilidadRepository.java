package com.spotterai.backend.repositories;

import com.spotterai.backend.models.Disponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Long> {
    
    // Devuelve todos los horarios de un usuario concreto
    List<Disponibilidad> findByUsuarioId(Long usuarioId);

    // Devuelve quiénes entrenan un día concreto de la semana
    List<Disponibilidad> findByDiaSemanaIgnoreCase(String diaSemana);

    // 🔥 NUEVO: Método para borrar los horarios antiguos de un plumazo
    @Modifying
    @Query("DELETE FROM Disponibilidad d WHERE d.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);
}