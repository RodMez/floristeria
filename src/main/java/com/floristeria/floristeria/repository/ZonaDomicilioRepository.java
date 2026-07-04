package com.floristeria.floristeria.repository;

import com.floristeria.floristeria.entity.ZonaDomicilio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ZonaDomicilioRepository extends JpaRepository<ZonaDomicilio, Integer> {
    List<ZonaDomicilio> findBySedeId(Integer sedeId);

    @Query(value = "SELECT COUNT(*) > 0 FROM Zonas_Domicilio " +
           "WHERE LOWER(unaccent(localidad)) = LOWER(unaccent(:localidad)) " +
           "AND LOWER(unaccent(COALESCE(barrio, ''))) = LOWER(unaccent(COALESCE(:barrio, ''))) " +
           "AND sede_id = :sedeId AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByLocalidadBarrioSede(@Param("localidad") String localidad,
                                        @Param("barrio") String barrio,
                                        @Param("sedeId") Integer sedeId);

    @Query(value = "SELECT COUNT(*) > 0 FROM Zonas_Domicilio " +
           "WHERE LOWER(unaccent(localidad)) = LOWER(unaccent(:localidad)) " +
           "AND LOWER(unaccent(COALESCE(barrio, ''))) = LOWER(unaccent(COALESCE(:barrio, ''))) " +
           "AND sede_id = :sedeId AND id != :id AND deleted_at IS NULL", nativeQuery = true)
    boolean existsByLocalidadBarrioSedeAndIdNot(@Param("localidad") String localidad,
                                                 @Param("barrio") String barrio,
                                                 @Param("sedeId") Integer sedeId,
                                                 @Param("id") Integer id);

    @Modifying
    @Query("UPDATE ZonaDomicilio z SET z.deletedAt = :now WHERE z.sede.id = :sedeId")
    void softDeleteBySedeId(@Param("sedeId") Integer sedeId, @Param("now") LocalDateTime now);
}
