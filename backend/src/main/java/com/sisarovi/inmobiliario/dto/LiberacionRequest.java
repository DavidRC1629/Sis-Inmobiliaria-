package com.sisarovi.inmobiliario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LiberacionRequest {
    @NotBlank(message = "La descripción de devolución es obligatoria")
    private String descripcion;

    private String adminPassword;
}