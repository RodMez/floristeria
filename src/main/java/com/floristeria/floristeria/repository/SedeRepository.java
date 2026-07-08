package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.Sede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SedeRepository extends JpaRepository<Sede, Integer> {

    /**
     * Verifica si ya existe una sede con la misma combinación de ciudad y nombre
     * (ignorando mayúsculas, minúsculas y tildes).
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM sedes " +
           "WHERE LOWER(unaccent(ciudad)) = LOWER(unaccent(:ciudad)) " +
           "AND LOWER(unaccent(nombre)) = LOWER(unaccent(:nombre)) " +
           "AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByCiudadAndNombreIgnoreCaseUnaccent(@Param("ciudad") String ciudad, @Param("nombre") String nombre);

    /**
     * Verifica si ya existe otra sede con la misma combinación de ciudad y nombre
     * (ignorando mayúsculas, minúsculas y tildes), excluyendo la sede con el id proporcionado.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM sedes " +
           "WHERE LOWER(unaccent(ciudad)) = LOWER(unaccent(:ciudad)) " +
           "AND LOWER(unaccent(nombre)) = LOWER(unaccent(:nombre)) " +
           "AND id != :id AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByCiudadAndNombreIgnoreCaseUnaccentAndIdNot(@Param("ciudad") String ciudad, @Param("nombre") String nombre, @Param("id") Integer id);
}