package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.CronogramaContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CronogramaContratoRepository extends JpaRepository<CronogramaContrato, Long> {

    Optional<CronogramaContrato> findByClienteId(Long clienteId);

    @Query("""
        SELECT DISTINCT c
        FROM CronogramaContrato c
        JOIN FETCH c.cliente cl
        JOIN FETCH cl.lote l
        LEFT JOIN FETCH c.cuotas q
        ORDER BY c.id DESC
    """)
    List<CronogramaContrato> findAllDetailed();
}
