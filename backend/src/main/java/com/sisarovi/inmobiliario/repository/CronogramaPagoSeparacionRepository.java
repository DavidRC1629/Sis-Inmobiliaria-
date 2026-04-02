package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.CronogramaPagoSeparacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CronogramaPagoSeparacionRepository extends JpaRepository<CronogramaPagoSeparacion, Long> {

    List<CronogramaPagoSeparacion> findByContratoIdOrderByIdAsc(Long contratoId);
}
