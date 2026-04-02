package com.sisarovi.inmobiliario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String dni;
    private String nombres;
    private String primerApellido;
    private String segundoApellido;
    private String role;
    private String estado;
    private boolean enabled;
}
