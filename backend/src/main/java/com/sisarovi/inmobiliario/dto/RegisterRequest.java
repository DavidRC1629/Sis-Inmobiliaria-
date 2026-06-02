package com.sisarovi.inmobiliario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String nombres;
    private String dni;
    private String email;
    private String password;
    private String primerApellido;
    private String segundoApellido;
}
