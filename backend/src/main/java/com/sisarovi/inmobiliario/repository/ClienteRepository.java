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
    boolean existsByEmailIgnoreCase(String email);

    Optional<Cliente> findByDni(String dni);
    Optional<Cliente> findByEmail(String email);
    Optional<Cliente> findByEmailIgnoreCase(String email);
    List<Cliente> findAllByLoteId(Long loteId);
    boolean existsByLoteId(Long loteId);
    boolean existsByLoteIdAndIdNot(Long loteId, Long id);

    @Query("SELECT c FROM Cliente c ORDER BY c.clienteDesde DESC, c.id DESC")
    List<Cliente> findAllOrdered();

    @Query("SELECT c FROM Cliente c WHERE c.lote.parcela.etapa.project.id = :projectId ORDER BY c.clienteDesde DESC, c.id DESC")
    List<Cliente> findByProjectIdOrdered(@Param("projectId") Long projectId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Cliente c WHERE c.lote.parcela.etapa.project.id = :projectId")
    boolean existsByProjectId(@Param("projectId") Long projectId);
}
