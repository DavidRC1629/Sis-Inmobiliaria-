package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.Etapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EtapaRepository extends JpaRepository<Etapa, Long> {
    
    List<Etapa> findByProjectIdOrderByNumeroEtapaAsc(Long projectId);
    
    boolean existsByProjectIdAndNumeroEtapa(Long projectId, Integer numeroEtapa);
    
    Long countByProjectId(Long projectId);
}
