package com.sisarovi.inmobiliario.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CronogramaPagoCuotaResponse {
    private Long id;
    private LocalDate fechaPago;
    private BigDecimal monto;
    private String tipoPago;
    private String estadoPago;
    private String notas;
}
