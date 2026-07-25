package com.spotterai.backend.repositories;

import com.spotterai.backend.models.Gimnasio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GimnasioRepository extends JpaRepository<Gimnasio, Long> {
    
    // Busca todos los gimnasios de una ciudad en concreto
    List<Gimnasio> findByCiudadIgnoreCase(String ciudad);

    // Busca gimnasios por código postal
    List<Gimnasio> findByCodigoPostal(String codigoPostal);
}