package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.UsuarioAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioAdminRepository extends JpaRepository<UsuarioAdmin, Integer> {

    /**
     * Busca un usuario por nombre
     */
    Optional<UsuarioAdmin> findByNombre(String nombre);

    /**
     * Busca un usuario por email
     */
    Optional<UsuarioAdmin> findByEmail(String email);

    /**
     * Lista todos los usuarios administradores que pertenecen a un sede en específico.
     */
    // CORREGIDO: Sede_Id
    List<UsuarioAdmin> findBySede_Id(Integer sedeId);
}