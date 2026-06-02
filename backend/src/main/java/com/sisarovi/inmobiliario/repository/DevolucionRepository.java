package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.Devolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {
    List<Devolucion> findAllByOrderByFechaCreacionDesc();

    List<Devolucion> findByEstadoIgnoreCaseOrderByFechaCreacionDesc(String estado);
}