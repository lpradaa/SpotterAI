package com.spotterai.backend.repositories;

import com.spotterai.backend.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByEmail(String email);

    // Devuelve a todos los usuarios menos al que está logueado.
    // El emparejamiento ya no se filtra en SQL: CalculadoraCompatibilidad puntua a
    // todos los candidatos y los ordena. El antiguo buscarMatches (mismo gimnasio
    // AND mismo nivel) descartaba de golpe a gente perfectamente compatible.
    List<Usuario> findByIdNot(Long id);

}