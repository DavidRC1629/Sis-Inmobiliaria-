package com.sisarovi.inmobiliario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteAdquisicionRequest {

    @NotNull(message = "El lote es obligatorio")
    private Long loteId;

    @NotBlank(message = "El tipo de operación es obligatorio")
    private String tipoOperacion;

    @NotNull(message = "La fecha de operación es obligatoria")
    private LocalDate fechaOperacion;

    @NotNull(message = "El precio de venta es obligatorio")
    private BigDecimal precioVenta;

    @NotNull(message = "El monto de operación es obligatorio")
    @DecimalMin(value = "100.01", message = "El monto de operación debe ser mayor que 100")
    private BigDecimal montoOperacion;

    @Size(max = 120, message = "El asesor no puede exceder 120 caracteres")
    private String asesor;

    @Size(max = 500, message = "El detalle de medios no puede exceder 500 caracteres")
    private String medios;

    private BigDecimal montoSeparacionObjetivo;

    private Integer plazoMeses;

    private BigDecimal interesPorcentaje;

    @NotEmpty(message = "Debe registrar al menos un propietario")
    @Valid
    private List<PropietarioRequest> propietarios;
}