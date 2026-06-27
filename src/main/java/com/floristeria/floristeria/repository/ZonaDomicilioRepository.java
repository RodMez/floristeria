package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.ZonaDomicilio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZonaDomicilioRepository extends JpaRepository<ZonaDomicilio, Integer> {
    List<ZonaDomicilio> findBySedeId(Integer sedeId);
}
