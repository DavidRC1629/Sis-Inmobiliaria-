package com.sisarovi.inmobiliario.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DevolucionResponse {
    private Long id;
    private Long loteId;
    private Integer loteNumero;
    private String manzana;
    private String parcelaNombre;
    private Integer etapaNumero;
    private String proyectoNombre;
    private BigDecimal montoTotal;
    private BigDecimal montoPagado;
    private BigDecimal montoPendiente;
    private Integer dias;
    private LocalDate fechaInicio;
    private LocalDate fechaFinEstimada;
    private String descripcion;
    private String estado;
    private Integer progreso;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private List<DevolucionPagoItemResponse> pagos;

    @Data
    @Builder
    public static class DevolucionPagoItemResponse {
        private Long id;
        private BigDecimal monto;
        private LocalDate fechaPago;
        private String descripcion;
        private String medioPago;
        private LocalDateTime fechaRegistro;
    }
}