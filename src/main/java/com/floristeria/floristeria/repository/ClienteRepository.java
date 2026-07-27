package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByFechaConsentimientoHabeasIsNull();

    @Modifying
    @Query(value = "UPDATE Clientes SET fecha_consentimiento_habeas = COALESCE(creado_en, CURRENT_TIMESTAMP), " +
            "version_politica_habeas = :version " +
            "WHERE fecha_consentimiento_habeas IS NULL AND deleted_at IS NULL", nativeQuery = true)
    int marcarConsentimientoLegacy(@Param("version") String version);
}
