package com.sisarovi.inmobiliario;

import com.sisarovi.inmobiliario.dto.AuthResponse;
import com.sisarovi.inmobiliario.dto.LoginRequest;
import com.sisarovi.inmobiliario.dto.RegisterRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void ensureAdminExists() {
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            Role adminRole = Role.builder().name("ROLE_ADMIN").build();
            roleRepository.save(adminRole);
        }

        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            Role userRole = Role.builder().name("ROLE_USER").build();
            roleRepository.save(userRole);
        }

        if (userRepository.findByDni("admin").isEmpty()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
            User admin = User.builder()
                    .dni("admin")
                    .email("admin@local.test")
                    .nombres("Admin")
                    .primerApellido("Test")
                    .segundoApellido("User")
                    .password(passwordEncoder.encode("admin123"))
                    .role(adminRole)
                    .estado(UserStatus.ACTIVO)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
        }
    }

    @Test
    void testRegisterFlow_newUser_returnsPendingToken() {
        // DNI max 8 chars (columna varchar(8))
        String shortId = UUID.randomUUID().toString().replace("-", "").substring(0, 7);

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setDni(shortId);
        registerRequest.setEmail("u-" + shortId + "@local.test");
        registerRequest.setPassword("Password123!");
        registerRequest.setNombres("Coverage");
        registerRequest.setPrimerApellido("User");
        registerRequest.setSegundoApellido("Test");

        // Un usuario nuevo queda en estado PENDIENTE — register devuelve token
        AuthResponse registerResponse = authService.register(registerRequest);
        assertNotNull(registerResponse);
        assertNotNull(registerResponse.getToken());
        assertEquals(shortId, registerResponse.getDni());
    }

    @Test
    void testLoginWithActiveAdmin_returnsToken() {
        // El admin se crea en @BeforeEach con estado ACTIVO → puede hacer login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier("admin");
        loginRequest.setPassword("admin123");

        AuthResponse loginResponse = authService.login(loginRequest);
        assertNotNull(loginResponse);
        assertNotNull(loginResponse.getToken());
        assertEquals("admin", loginResponse.getDni());
    }

    @Test
    void testGetCurrentUserProfileReturnsExistingUser() {
        AuthResponse response = authService.getCurrentUserProfile("admin");
        assertNotNull(response);
        assertEquals("admin", response.getDni());
    }
}
