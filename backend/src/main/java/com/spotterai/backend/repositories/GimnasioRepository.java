package com.spotterai.backend.repositories;

import com.spotterai.backend.textos.ErrorDeNegocio;
import com.spotterai.backend.models.Gimnasio;
import com.spotterai.backend.services.NombreDeGimnasio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GimnasioRepository extends JpaRepository<Gimnasio, Long> {

    // Busca todos los gimnasios de una ciudad en concreto
    List<Gimnasio> findByCiudadIgnoreCase(String ciudad);

    // Busca gimnasios por código postal
    List<Gimnasio> findByCodigoPostal(String codigoPostal);

    /**
     * El gimnasio que se llama asi, creandolo si no habia ninguno.
     *
     * <p>Vive aqui y no en {@code GimnasioServiceImpl} —que es quien lo expone—
     * porque quien lo necesita de verdad es el guardado del perfil, y su
     * servicio ya recibe doce dependencias: la decimotercera para envolver un
     * findAll y un save no compensa. La regla de cuando dos nombres son el
     * mismo esta aparte, en {@link NombreDeGimnasio}, que es lo unico de esto
     * que es una decision y no una consulta.
     *
     * <p>Se compara en Java sobre el catalogo entero en vez de en SQL: hacerlo
     * en la consulta pediria una segunda columna con el nombre ya reducido, a
     * mantener en cada escritura. El catalogo es pequeño por definicion —se
     * pinta entero en un desplegable— y ya se carga asi para ese desplegable.
     *
     * @throws IllegalArgumentException si el nombre viene vacio
     */
    default Gimnasio buscarOCrear(String nombre) {
        String normalizado = NombreDeGimnasio.normalizar(nombre);
        if (normalizado.isEmpty()) {
            throw ErrorDeNegocio.de("error.gimnasio.sinNombre");
        }

        return findAll().stream()
                .filter(g -> NombreDeGimnasio.normalizar(g.getNombre()).equals(normalizado))
                .findFirst()
                .orElseGet(() -> {
                    Gimnasio nuevo = new Gimnasio();
                    // Se guarda lo que escribio la persona, no la forma reducida:
                    // esa solo sirve para decidir si ya existia.
                    nuevo.setNombre(nombre.trim().replaceAll("\\s+", " "));
                    return save(nuevo);
                });
    }
}