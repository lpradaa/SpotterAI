package com.spotterai.backend.repositories;

import com.spotterai.backend.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByEmail(String email);

    /**
     * Quien tiene esa llave de baja, si alguien la tiene.
     *
     * <p>Es lo unico que se puede hacer con la llave. No devuelve una sesion ni
     * autentica a nadie: solo identifica de quien es la preferencia que se va a
     * cambiar, porque esa llave viaja en el cuerpo de un correo.
     */
    Optional<Usuario> findByTokenAvisos(String tokenAvisos);

    // Devuelve a todos los usuarios menos al que está logueado.
    // El emparejamiento ya no se filtra en SQL: CalculadoraCompatibilidad puntua a
    // todos los candidatos y los ordena. El antiguo buscarMatches (mismo gimnasio
    // AND mismo nivel) descartaba de golpe a gente perfectamente compatible.
    List<Usuario> findByIdNot(Long id);

}