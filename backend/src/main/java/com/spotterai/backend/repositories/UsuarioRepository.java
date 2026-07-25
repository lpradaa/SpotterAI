package com.spotterai.backend.repositories;

import com.spotterai.backend.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByEmail(String email);

    // 🔥 SOLUCIÓN DEFINITIVA EN JPQL (Carga los objetos anidados como Gimnasio correctamente)
    @Query("SELECT DISTINCT u FROM Usuario u " +
           "WHERE u.gimnasio.id = :gimId " +
           "AND u.id != :miId " +
           "AND u.nivel = :miNivel " +
           "AND u.id NOT IN (SELECT s.emisor.id FROM Solicitud s WHERE s.receptor.id = :miId) " +
           "AND u.id NOT IN (SELECT s.receptor.id FROM Solicitud s WHERE s.emisor.id = :miId)")
    List<Usuario> buscarMatches(@Param("gimId") Long gimId, 
                                @Param("miId") Long miId, 
                                @Param("miNivel") String miNivel);

       // 🔥 NUEVO: Devuelve a todos los usuarios menos al que está logueado
    List<Usuario> findByIdNot(Long id);
    
}