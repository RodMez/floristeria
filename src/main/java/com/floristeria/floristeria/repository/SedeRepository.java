package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.Sede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SedeRepository extends JpaRepository<Sede, Integer> {

    /**
     * Busca una sede por su ciudad ignorando mayúsculas/minúsculas.
     * Útil para el enrutamiento del frontend según la ciudad seleccionada.
     */
    Optional<Sede> findByCiudadIgnoreCase(String ciudad);

    /**
     * Verifica si ya existe una sede con la misma ciudad (ignorando mayúsculas, minúsculas y tildes).
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM sedes " +
           "WHERE LOWER(unaccent(ciudad)) = LOWER(unaccent(:ciudad)) " +
           "AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByCiudadIgnoreCaseUnaccent(@Param("ciudad") String ciudad);

    /**
     * Verifica si ya existe otra sede con la misma ciudad (ignorando mayúsculas, minúsculas y tildes),
     * excluyendo la sede con el id proporcionado.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM sedes " +
           "WHERE LOWER(unaccent(ciudad)) = LOWER(unaccent(:ciudad)) " +
           "AND id != :id AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByCiudadIgnoreCaseUnaccentAndIdNot(@Param("ciudad") String ciudad, @Param("id") Integer id);
}