package com.sisarovi.inmobiliario.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CronogramaContratoResponse {
    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private String clienteDni;
    private String asesor;
    private Long projectId;
    private String projectNombre;
    private Integer etapaNumero;
    private String parcelaNombre;
    private String manzana;
    private Long loteId;
    private Integer loteNumero;
    private String tipoOperacion;
    private String estado;
    private LocalDate fechaOperacion;
    private LocalDate fechaInicioCronograma;
    private BigDecimal precioVenta;
    private BigDecimal montoPagadoTotal;
    private BigDecimal montoSeparacionObjetivo;
    private BigDecimal montoSeparacionAcumulado;
    private BigDecimal saldoFinanciarInicial;
    private BigDecimal saldoPendienteActual;
    private Integer plazoMeses;
    private BigDecimal interesPorcentaje;
    private BigDecimal montoCuotaReferencial;
    private List<CronogramaPagoCuotaResponse> pagosSeparacion;
    private List<CronogramaCuotaResponse> cuotas;
}
