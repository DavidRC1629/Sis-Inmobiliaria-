package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByDni(String dni);
    boolean existsByEmail(String email);

    Optional<Cliente> findByDni(String dni);
    Optional<Cliente> findByEmail(String email);
    List<Cliente> findAllByLoteId(Long loteId);
    boolean existsByLoteId(Long loteId);
    boolean existsByLoteIdAndIdNot(Long loteId, Long id);

    @Query("SELECT c FROM Cliente c ORDER BY c.clienteDesde DESC, c.id DESC")
    List<Cliente> findAllOrdered();

    @Query("SELECT c FROM Cliente c WHERE c.lote.parcela.etapa.project.id = :projectId ORDER BY c.clienteDesde DESC, c.id DESC")
    List<Cliente> findByProjectIdOrdered(@Param("projectId") Long projectId);
}
