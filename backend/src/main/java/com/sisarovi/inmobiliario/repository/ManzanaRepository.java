package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.entity.Manzana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManzanaRepository extends JpaRepository<Manzana, Long> {
    List<Manzana> findByParcelaIdOrderByNombreAsc(Long parcelaId);

    List<Manzana> findByParcelaIdOrderByIdAsc(Long parcelaId);

    void deleteAllByParcelaId(Long parcelaId);
}