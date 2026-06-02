package com.sisarovi.inmobiliario.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TerrenoPropioRequest {
    @NotNull
    private Integer numeroLote;
    private String calle;
    private BigDecimal areaM2;
    private BigDecimal perimetro;
    private BigDecimal medidaFrente;
    private BigDecimal medidaFondo;
    private BigDecimal medidaIzquierda;
    private BigDecimal medidaDerecha;
    @NotBlank
    @Size(max = 8, message = "El número de partida debe tener como máximo 8 dígitos")
    @Pattern(regexp = "^[0-9]+$", message = "El número de partida solo debe contener números")
    private String numeroPartida;
    @NotNull
    private BigDecimal precio;
    @NotNull
    private Long propietarioId;
    private String imagenUrl;
}
