package com.sisarovi.inmobiliario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRequest {
    
    @NotBlank(message = "El nombre del proyecto es obligatorio")
    private String nombre;
    
    private String imagenUrl;

    private String logoUrl;
    
    @NotNull(message = "La cantidad de etapas es obligatoria")
    @Min(value = 1, message = "Debe tener al menos 1 etapa")
    private Integer cantidadEtapas;
}
