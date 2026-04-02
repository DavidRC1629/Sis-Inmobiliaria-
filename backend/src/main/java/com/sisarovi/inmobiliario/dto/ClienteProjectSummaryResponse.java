package com.sisarovi.inmobiliario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteProjectSummaryResponse {
    private Long projectId;
    private String projectNombre;
    private Integer cantidadClientes;
}