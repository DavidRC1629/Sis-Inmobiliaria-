package com.sisarovi.inmobiliario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParcelaResponse {
    
    private Long id;
    private String nombre;
    private Integer numManzanas;
    private String propietario;
    private Integer cantidadLotes;
    private Integer lotesDisponibles;
    private Long etapaId;
}
