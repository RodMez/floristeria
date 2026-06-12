package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, Integer> {

    List<Direccion> findByClienteId(Integer clienteId);
}
