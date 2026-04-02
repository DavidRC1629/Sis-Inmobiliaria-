package com.sisarovi.inmobiliario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {
    
    private Long id;
    private String nombre;
    private String imagenUrl;
    private String logoUrl;
        private String createdByNombre;
    private Integer cantidadEtapas;
    private Integer cantidadParcelasTotal;
}
