package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.FechaBloqueadaSede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FechaBloqueadaSedeRepository extends JpaRepository<FechaBloqueadaSede, Integer> {

    boolean existsBySede_IdAndFecha(Integer sedeId, LocalDate fecha);

    List<FechaBloqueadaSede> findBySede_IdAndFechaBetween(Integer sedeId, LocalDate desde, LocalDate hasta);

    List<FechaBloqueadaSede> findBySede_IdOrderByFechaAsc(Integer sedeId);
}
