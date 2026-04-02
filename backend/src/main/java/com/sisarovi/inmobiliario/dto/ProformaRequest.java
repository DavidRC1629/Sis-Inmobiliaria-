package com.sisarovi.inmobiliario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProformaRequest {
    private String codigo;
    private String proyecto;
    private String clienteNombre;
    private String clienteDni;
    private String clienteCelular;
    private String asesor;
    private String fechaEmision;
    private String fechaVencimiento;
    private BigDecimal precioContado;
    private Object detalle;
}
