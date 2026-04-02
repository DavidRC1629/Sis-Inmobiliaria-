package com.sisarovi.inmobiliario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proformas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proforma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(length = 200)
    private String proyecto;

    @Column(name = "cliente_nombre", length = 40)
    private String clienteNombre;

    @Column(name = "cliente_dni", length = 20)
    private String clienteDni;

    @Column(name = "cliente_celular", length = 30)
    private String clienteCelular;

    @Column(length = 150)
    private String asesor;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "precio_contado", precision = 12, scale = 2)
    private BigDecimal precioContado;

    @Column(name = "detalle_json", columnDefinition = "LONGTEXT")
    private String detalleJson;

    @Lob
    @Column(name = "pdf_data", columnDefinition = "LONGBLOB")
    private byte[] pdfData;

    @Column(name = "pdf_file_name", length = 255)
    private String pdfFileName;

    @Column(name = "pdf_content_type", length = 120)
    private String pdfContentType;

    @Column(name = "created_by", length = 20)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
