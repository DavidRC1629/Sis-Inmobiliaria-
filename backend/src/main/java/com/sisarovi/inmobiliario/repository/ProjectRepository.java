package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    List<Project> findByCreatedById(Long userId);
    
    List<Project> findByNombreContainingIgnoreCase(String nombre);
}
