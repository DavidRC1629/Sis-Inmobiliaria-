package com.sisarovi.inmobiliario.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CronogramaDescuentoRequest {
    private Long clienteId;
    private BigDecimal montoDescuento;
    private String observacion;
}
