package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    /**
     * Busca una categoría por su nombre exacto para evitar duplicados al crear.
     */
    Optional<Categoria> findByNombre(String nombre);
}