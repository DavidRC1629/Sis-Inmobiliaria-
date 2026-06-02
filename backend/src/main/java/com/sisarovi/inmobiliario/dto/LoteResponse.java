package com.sisarovi.inmobiliario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteResponse {
    
    private Long id;
    private Integer numero;
    private String calle;
    private BigDecimal perimetro;
    private BigDecimal areaM2;
    private BigDecimal medidaFrente;
    private BigDecimal medidaIzquierda;
    private BigDecimal medidaDerecha;
    private BigDecimal medidaFondo;
    private String numeroPartida;
    private BigDecimal precioLote;
    private Long manzanaId;
    private String manzana;
    private Long parcelaId;
    private String parcelaNombre;
    private Integer etapaNumero;
    private Long projectId;
    private String projectNombre;
    private boolean adquirido;
}
