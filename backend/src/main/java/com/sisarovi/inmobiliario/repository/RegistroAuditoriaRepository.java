package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {
	List<RegistroAuditoria> findAllByOrderByFechaHoraDesc();

	Optional<RegistroAuditoria> findTopByUsuarioAndAccionOrderByFechaHoraDesc(String usuario, String accion);

	List<RegistroAuditoria> findByFechaHoraBetween(LocalDateTime start, LocalDateTime end);

	List<RegistroAuditoria> findByFechaHoraGreaterThanEqualOrderByFechaHoraDesc(LocalDateTime fecha);

	List<RegistroAuditoria> findByFechaHoraLessThanEqualOrderByFechaHoraDesc(LocalDateTime fecha);
}
