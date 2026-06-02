package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.CronogramaContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CronogramaContratoRepository extends JpaRepository<CronogramaContrato, Long> {

    Optional<CronogramaContrato> findByClienteId(Long clienteId);

    List<CronogramaContrato> findByClienteIdIn(List<Long> clienteIds);

    @Query("""
        SELECT DISTINCT c
        FROM CronogramaContrato c
        JOIN FETCH c.cliente cl
        JOIN FETCH cl.lote l
        LEFT JOIN FETCH c.cuotas q
        WHERE l.id IN :loteIds
        ORDER BY c.fechaOperacion DESC, c.id DESC
    """)
    List<CronogramaContrato> findDetailedByLoteIds(@Param("loteIds") List<Long> loteIds);

    @Transactional
    void deleteByClienteIdIn(List<Long> clienteIds);

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
