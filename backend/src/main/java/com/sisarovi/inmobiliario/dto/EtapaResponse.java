package com.sisarovi.inmobiliario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtapaResponse {
    
    private Long id;
    private Integer numeroEtapa;
    private Integer cantidadParcelas;
    private Long projectId;
}
