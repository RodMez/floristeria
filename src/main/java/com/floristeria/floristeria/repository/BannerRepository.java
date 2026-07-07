package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Integer> {

    @Query("SELECT b FROM Banner b WHERE b.ubicacion = :ubicacion AND b.sedeId IS NULL AND b.activo = true ORDER BY b.orden ASC")
    List<Banner> findActivosByUbicacionGlobal(@Param("ubicacion") String ubicacion);

    @Query("SELECT b FROM Banner b WHERE b.ubicacion = :ubicacion AND b.sedeId = :sedeId AND b.activo = true ORDER BY b.orden ASC")
    List<Banner> findActivosByUbicacionAndSedeId(@Param("ubicacion") String ubicacion, @Param("sedeId") Integer sedeId);

    @Query("SELECT COUNT(b) FROM Banner b WHERE b.ubicacion = :ubicacion AND (b.sedeId IS NULL OR b.sedeId = :sedeId) AND b.activo = true")
    long countActivosByUbicacionAndSede(@Param("ubicacion") String ubicacion, @Param("sedeId") Integer sedeId);

    @Query("SELECT b FROM Banner b ORDER BY b.ubicacion ASC, b.sedeId ASC NULLS FIRST, b.orden ASC")
    List<Banner> findAllOrdered();

    Optional<Banner> findByIdAndDeletedAtIsNull(Integer id);
}
