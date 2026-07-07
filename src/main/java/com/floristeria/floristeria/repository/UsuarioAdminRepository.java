package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.UsuarioAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
     * Verifica si existe un usuario con el email dado
     */
    boolean existsByEmail(String email);

    /**
     * Lista todos los usuarios administradores que pertenecen a un sede en específico.
     */
    // CORREGIDO: Sede_Id
    List<UsuarioAdmin> findBySede_Id(Integer sedeId);

    @Modifying
    @Query("UPDATE UsuarioAdmin u SET u.deletedAt = :now WHERE u.sede.id = :sedeId")
    void softDeleteBySedeId(@Param("sedeId") Integer sedeId, @Param("now") LocalDateTime now);
}