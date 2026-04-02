package com.sisarovi.inmobiliario.dto;

import lombok.Data;

@Data
public class CronogramaFilterRequest {
    private Long projectId;
    private Integer etapaNumero;
    private String parcelaNombre;
    private String manzana;
    private Long loteId;
    private String dni;
    private String nombres;
    private String estado;
}
