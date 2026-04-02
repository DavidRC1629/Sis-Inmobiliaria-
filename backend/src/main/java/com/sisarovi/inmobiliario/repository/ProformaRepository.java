package com.sisarovi.inmobiliario.repository;

import com.sisarovi.inmobiliario.dto.ProformaResponse;
import com.sisarovi.inmobiliario.entity.Proforma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProformaRepository extends JpaRepository<Proforma, Long> {
    @Query("""
            SELECT new com.sisarovi.inmobiliario.dto.ProformaResponse(
                p.id,
                p.codigo,
                p.proyecto,
                p.clienteNombre,
                p.clienteDni,
                p.asesor,
                p.fechaEmision,
                p.fechaVencimiento,
                p.precioContado,
                p.createdAt,
                CASE WHEN p.pdfFileName IS NOT NULL AND p.pdfFileName <> '' THEN true ELSE false END
            )
            FROM Proforma p
            ORDER BY p.createdAt DESC
            """)
    List<ProformaResponse> findHistorialLite();

    @Query("""
            SELECT new com.sisarovi.inmobiliario.dto.ProformaResponse(
                p.id,
                p.codigo,
                p.proyecto,
                p.clienteNombre,
                p.clienteDni,
                p.asesor,
                p.fechaEmision,
                p.fechaVencimiento,
                p.precioContado,
                p.createdAt,
                CASE WHEN p.pdfFileName IS NOT NULL AND p.pdfFileName <> '' THEN true ELSE false END
            )
            FROM Proforma p
            WHERE REPLACE(UPPER(p.codigo), '-', '') LIKE CONCAT('%', UPPER(:codigoNormalizado), '%')
            ORDER BY p.createdAt DESC
            """)
    List<ProformaResponse> buscarTop50PorCodigoNormalizado(@Param("codigoNormalizado") String codigoNormalizado);

    @Query("""
            SELECT new com.sisarovi.inmobiliario.dto.ProformaResponse(
                p.id,
                p.codigo,
                p.proyecto,
                p.clienteNombre,
                p.clienteDni,
                p.asesor,
                p.fechaEmision,
                p.fechaVencimiento,
                p.precioContado,
                p.createdAt,
                CASE WHEN p.pdfFileName IS NOT NULL AND p.pdfFileName <> '' THEN true ELSE false END
            )
            FROM Proforma p
            WHERE UPPER(p.clienteNombre) LIKE CONCAT('%', UPPER(:clienteNombre), '%')
            ORDER BY p.createdAt DESC
            """)
    List<ProformaResponse> findTop50ByClienteNombreContainingIgnoreCaseOrderByCreatedAtDesc(@Param("clienteNombre") String clienteNombre);

    boolean existsByCodigo(String codigo);
}
