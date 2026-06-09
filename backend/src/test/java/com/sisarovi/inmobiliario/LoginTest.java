package com.sisarovi.inmobiliario;

import com.sisarovi.inmobiliario.dto.LoginRequest;
import com.sisarovi.inmobiliario.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
class LoginTest {

    @Autowired
    private AuthService authService;

    @Test
    void testLoginWithValidCredentials() {
        // 1. Instanciamos el DTO
        LoginRequest request = new LoginRequest();
        
        // 2. Usamos los setters de forma correcta (sin los nombres de parámetros internos)
        request.setIdentifier("00000000");
        request.setPassword("admin123");

        // 3. Ejecutamos la lógica del servicio
        var response = authService.login(request);

        // 4. Verificamos que la respuesta no sea nula
        assertNotNull(response, "La respuesta del login no debería ser nula");
    }
}