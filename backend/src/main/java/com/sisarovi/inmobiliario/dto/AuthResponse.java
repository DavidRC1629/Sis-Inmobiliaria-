package com.sisarovi.inmobiliario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String dni;
    private String email;
    private String nombres;
    private String primerApellido;
    private String segundoApellido;
    private String role;
    private String message;
    private Boolean requirePasswordChange;
}
