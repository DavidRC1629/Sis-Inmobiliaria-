package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.Parcela;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParcelaRepository extends JpaRepository<Parcela, Long> {
    
    List<Parcela> findByEtapaIdOrderByNombreAsc(Long etapaId);
    
    List<Parcela> findByPropietarioContainingIgnoreCase(String propietario);
}
