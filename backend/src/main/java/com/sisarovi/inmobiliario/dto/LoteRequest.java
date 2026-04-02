package com.sisarovi.inmobiliario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteRequest {
    
    @NotNull(message = "El número del lote es obligatorio")
    @Positive(message = "El número del lote debe ser positivo")
    private Integer numero;
    
    private String calle;
    
    private BigDecimal perimetro;
    
    private BigDecimal areaM2;
    
    private BigDecimal medidaFrente;
    
    private BigDecimal medidaIzquierda;
    
    private BigDecimal medidaDerecha;
    
    private BigDecimal medidaFondo;
    
    @NotBlank(message = "El número de partida es obligatorio")
    private String numeroPartida;

    @NotNull(message = "El precio del lote es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio del lote debe ser mayor a 0")
    private BigDecimal precioLote;
    
    private Long manzanaId;

    private String manzana;
}
