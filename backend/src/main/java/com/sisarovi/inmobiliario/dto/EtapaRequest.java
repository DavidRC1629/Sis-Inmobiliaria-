package com.sisarovi.inmobiliario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EtapaRequest {
    
    @NotNull(message = "El número de etapa es obligatorio")
    @Positive(message = "El número de etapa debe ser positivo")
    private Integer numeroEtapa;
}
