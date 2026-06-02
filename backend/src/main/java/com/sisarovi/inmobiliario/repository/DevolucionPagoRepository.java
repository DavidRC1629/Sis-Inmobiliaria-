package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.DevolucionPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DevolucionPagoRepository extends JpaRepository<DevolucionPago, Long> {
    List<DevolucionPago> findByDevolucionIdOrderByFechaRegistroDesc(Long devolucionId);
}