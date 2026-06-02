package com.sisarovi.inmobiliario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DevolucionRequest {
    @NotNull
    private Long loteId;

    @NotNull
    private Integer loteNumero;

    @NotBlank
    private String manzana;

    @NotBlank
    private String parcelaNombre;

    @NotNull
    private Integer etapaNumero;

    @NotBlank
    private String proyectoNombre;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal montoTotal;

    @NotNull
    private LocalDate fechaInicio;

    @NotNull
    private LocalDate fechaFinEstimada;

    @NotNull
    private Integer dias;

    @NotBlank
    private String descripcion;
}