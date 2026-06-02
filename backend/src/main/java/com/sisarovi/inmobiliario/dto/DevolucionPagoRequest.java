package com.sisarovi.inmobiliario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DevolucionPagoRequest {
    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal monto;

    @NotNull
    private LocalDate fechaPago;

    @NotBlank
    private String descripcion;

    @NotBlank
    private String medioPago;
}