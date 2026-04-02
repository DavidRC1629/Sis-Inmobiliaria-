package com.sisarovi.inmobiliario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteResponse {
    private Long id;
    private String nombres;
    private String apellidos;
    private String dni;
    private String email;
    private String telefono;
    private String direccion;
    private String tipoRelacion;
    private LocalDate clienteDesde;

    private Long projectId;
    private String projectNombre;
    private Integer etapaNumero;
    private String parcelaNombre;
    private String manzana;
    private Long loteId;
    private Integer loteNumero;
}