package com.sisarovi.inmobiliario.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParcelaRequest {
    
    @NotBlank(message = "El nombre de la parcela es obligatorio")
    private String nombre;
    
    @NotNull(message = "El número de manzanas es obligatorio")
    @Min(value = 1, message = "Debe haber al menos 1 manzana")
    @Max(value = 27, message = "El máximo de manzanas es 27 (A-Z)")
    private Integer numManzanas;
    
    @NotBlank(message = "El propietario es obligatorio")
    private String propietario;
}
