package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.Sede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SedeRepository extends JpaRepository<Sede, Integer> {

    /**
     * Busca una sede por su ciudad ignorando mayúsculas/minúsculas.
     * Útil para el enrutamiento del frontend según la ciudad seleccionada.
     */
    Optional<Sede> findByCiudadIgnoreCase(String ciudad);
}