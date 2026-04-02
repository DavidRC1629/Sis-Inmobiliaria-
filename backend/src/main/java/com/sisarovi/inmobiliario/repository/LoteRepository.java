package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {
            java.util.Optional<Lote> findByParcelaIdAndManzana_IdAndNumero(Long parcelaId, Long manzanaId, Integer numero);
        List<Lote> findByManzanaIdOrderByNumeroAsc(Long manzanaId);
    
    List<Lote> findByParcelaIdOrderByNumeroAsc(Long parcelaId);
    
    List<Lote> findByParcelaIdAndManzanaIdOrderByNumeroAsc(Long parcelaId, Long manzanaId);
    
    Optional<Lote> findByParcelaIdAndNumero(Long parcelaId, Integer numero);
    
    
    Optional<Lote> findByNumeroPartida(String numeroPartida);

    Optional<Lote> findByNumeroPartidaIgnoreCase(String numeroPartida);
    
    @Query("SELECT l FROM Lote l WHERE l.numeroPartida = :numeroPartida AND l.parcela.etapa.project.id = :projectId")
    Optional<Lote> findByNumeroPartidaAndProjectId(@Param("numeroPartida") String numeroPartida, @Param("projectId") Long projectId);

    @Query("SELECT l FROM Lote l WHERE l.parcela.etapa.project.id = :projectId ORDER BY l.parcela.etapa.numeroEtapa ASC, l.parcela.nombre ASC, l.manzana.nombre ASC, l.numero ASC")
    List<Lote> findByProjectIdForClientes(@Param("projectId") Long projectId);
    
    void deleteAllByParcelaId(Long parcelaId);
}
