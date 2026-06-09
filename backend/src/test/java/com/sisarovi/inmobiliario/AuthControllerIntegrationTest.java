package com.sisarovi.inmobiliario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.LoginRequest;
import com.sisarovi.inmobiliario.repository.RoleRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setupAdmin() {
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            roleRepository.save(com.sisarovi.inmobiliario.entity.Role.builder().name("ROLE_ADMIN").build());
        }
        if (userRepository.findByDni("admin").isEmpty()) {
            com.sisarovi.inmobiliario.entity.Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();
            userRepository.save(com.sisarovi.inmobiliario.entity.User.builder()
                    .dni("admin")
                    .email("admin@local.test")
                    .nombres("Admin")
                    .primerApellido("Test")
                    .segundoApellido("User")
                    .password(passwordEncoder.encode("admin123"))
                    .role(adminRole)
                    .estado(com.sisarovi.inmobiliario.entity.UserStatus.ACTIVO)
                    .enabled(true)
                    .build());
        }
    }

    @Test
    void testLoginEndpointReturnsToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier("admin");
        loginRequest.setPassword("admin123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(loginRequest), headers);

        ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:" + port + "/api/auth/login", request, String.class);
        assertNotNull(response.getBody());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertNotNull(responseBody.get("token"));
        assertEquals("admin", responseBody.get("dni").asText());
    }
}
