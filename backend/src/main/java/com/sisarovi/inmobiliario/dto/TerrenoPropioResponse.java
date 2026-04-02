package com.sisarovi.inmobiliario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TerrenoPropioResponse {
    private Long id;
    private Integer numeroLote;
    private String calle;
    private BigDecimal areaM2;
    private BigDecimal perimetro;
    private BigDecimal medidaFrente;
    private BigDecimal medidaFondo;
    private BigDecimal medidaIzquierda;
    private BigDecimal medidaDerecha;
    private String numeroPartida;
    private BigDecimal precio;
    private ClienteResponse propietario;
    private String imagenUrl;
    private String estado;
}
