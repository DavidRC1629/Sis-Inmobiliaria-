package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    List<Project> findByCreatedById(Long userId);
    
    List<Project> findByNombreContainingIgnoreCase(String nombre);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Project p WHERE LOWER(TRIM(p.nombre)) = LOWER(TRIM(:nombre))")
    boolean existsByNombreNormalized(@Param("nombre") String nombre);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Project p WHERE LOWER(TRIM(p.nombre)) = LOWER(TRIM(:nombre)) AND p.id <> :projectId")
    boolean existsByNombreNormalizedAndIdNot(@Param("nombre") String nombre, @Param("projectId") Long projectId);
}
