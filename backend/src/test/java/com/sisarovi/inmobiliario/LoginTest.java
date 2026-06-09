package com.sisarovi.inmobiliario;

import com.sisarovi.inmobiliario.dto.LoginRequest;
import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.repository.RoleRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import com.sisarovi.inmobiliario.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoginTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Crear rol ADMIN si no existe
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            roleRepository.save(Role.builder().name("ROLE_ADMIN").build());
        }
        // Crear usuario admin con DNI 00000000 si no existe
        if (userRepository.findByDni("00000000").isEmpty()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
            userRepository.save(User.builder()
                    .dni("00000000")
                    .email("admin@test.com")
                    .nombres("Admin")
                    .primerApellido("Test")
                    .segundoApellido("User")
                    .password(passwordEncoder.encode("admin123"))
                    .role(adminRole)
                    .estado(UserStatus.ACTIVO)
                    .enabled(true)
                    .build());
        }
    }

    @Test
    void testLoginWithValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("00000000");
        request.setPassword("admin123");

        var response = authService.login(request);

        assertNotNull(response, "La respuesta del login no debería ser nula");
    }
}
