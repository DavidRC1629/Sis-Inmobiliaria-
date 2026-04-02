package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.CronogramaCuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CronogramaCuotaRepository extends JpaRepository<CronogramaCuota, Long> {
}
