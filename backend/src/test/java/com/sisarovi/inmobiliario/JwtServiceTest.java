package com.sisarovi.inmobiliario;

import com.sisarovi.inmobiliario.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void testGenerateTokenAndValidate() {
        UserDetails userDetails = User.withUsername("coverage-user")
                .password("ignored")
                .roles("USER")
                .build();

        String token = jwtService.generateToken(Map.of("role", "ROLE_USER"), userDetails);

        assertNotNull(token, "El token no debe ser nulo");
        assertEquals("coverage-user", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }
}
