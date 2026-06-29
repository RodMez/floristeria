package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.ConfiguracionTienda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionTiendaRepository extends JpaRepository<ConfiguracionTienda, Integer> {
}
