package com.sisarovi.inmobiliario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteLoteOptionResponse {
    private Long loteId;
    private Integer loteNumero;
    private String manzana;
    private String parcelaNombre;
    private Integer etapaNumero;
    private Long projectId;
    private String projectNombre;
}