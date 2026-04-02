package com.sisarovi.inmobiliario.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdquisicionTerrenoRequest {
    @NotNull
    private Long clienteId;
    @NotNull
    private String formaPago; // CONTADO o CUOTAS
    private Integer cuotas; // Solo si es cuotas
    private Double interes; // Solo si es cuotas
}
