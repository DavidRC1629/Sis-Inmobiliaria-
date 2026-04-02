package com.sisarovi.inmobiliario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProformaResponse {
    private Long id;
    private String codigo;
    private String proyecto;
    private String clienteNombre;
    private String clienteDni;
    private String asesor;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private BigDecimal precioContado;
    private LocalDateTime createdAt;
    private Boolean hasPdf;
}
