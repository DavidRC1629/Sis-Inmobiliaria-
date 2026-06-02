package com.sisarovi.inmobiliario.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LiberacionLoteResponse {
    private Long loteId;
    private Integer loteNumero;
    private String manzana;
    private String parcelaNombre;
    private Long parcelaId;
    private Integer etapaNumero;
    private Long etapaId;
    private Long projectId;
    private String projectNombre;

    private String titulares;
    private String titularesDni;
    private Integer cantidadTitulares;

    private Long contratoId;
    private String tipoOperacion;
    private String estadoCronograma;
    private String estadoVisual;
    private boolean moroso;
    private BigDecimal montoPagadoTotal;
    private boolean requierePasswordAdmin;
}