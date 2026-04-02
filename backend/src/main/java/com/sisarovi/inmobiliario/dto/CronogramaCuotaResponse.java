package com.sisarovi.inmobiliario.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CronogramaCuotaResponse {
    private Long id;
    private Integer numeroCuota;
    private LocalDate fechaVencimiento;
    private BigDecimal montoCuota;
    private BigDecimal montoPagado;
    private BigDecimal saldoPendiente;
    private String estadoPago;
    private Integer diasRetraso;
    private LocalDate fechaPago;
    private List<CronogramaPagoCuotaResponse> pagos;
}
