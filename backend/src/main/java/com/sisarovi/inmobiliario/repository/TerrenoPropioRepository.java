package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.TerrenoPropio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TerrenoPropioRepository extends JpaRepository<TerrenoPropio, Long> {
    Optional<TerrenoPropio> findByNumeroPartida(String numeroPartida);
    boolean existsByNumeroPartida(String numeroPartida);
}
