package com.sisarovi.inmobiliario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.DeleteUserRequest;
import com.sisarovi.inmobiliario.dto.LoginRequest;
import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.entity.UserStatus;
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

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ActiveProfiles("test")
class AdminControllerIntegrationTest {

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

    private String adminToken;
    private User adminUser;

    @BeforeEach
    void setupAdmin() throws Exception {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));
        if (userRepository.findByDni("admin").isEmpty()) {
            adminUser = userRepository.save(User.builder()
                    .dni("admin")
                    .email("admin@local.test")
                    .nombres("Admin")
                    .primerApellido("Test")
                    .segundoApellido("User")
                    .password(passwordEncoder.encode("admin123"))
                    .role(adminRole)
                    .estado(UserStatus.ACTIVO)
                    .enabled(true)
                    .build());
        } else {
            adminUser = userRepository.findByDni("admin").orElseThrow();
        }

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setIdentifier("admin");
        loginRequest.setPassword("admin123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(loginRequest), headers);
        ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:" + port + "/api/auth/login", request, String.class);
        assertNotNull(response.getBody());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        adminToken = responseBody.get("token").asText();
    }

    private User createPendingUser() {
        Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
        String randomDni = String.format("%08d", ThreadLocalRandom.current().nextInt(10_000_000));
        return userRepository.save(User.builder()
                .dni(randomDni)
                .email("pending-" + randomDni + "@local.test")
                .nombres("Pending")
                .primerApellido("User")
                .segundoApellido("Test")
                .password(passwordEncoder.encode("Password123!"))
                .role(userRole)
                .estado(UserStatus.PENDIENTE)
                .enabled(true)
                .build());
    }

    @Test
    void testAdminGetPendingUsersAndApproveReject() throws Exception {
        User pendingUser = createPendingUser();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> pendingResponse = restTemplate.exchange("http://localhost:" + port + "/api/admin/users/pending", org.springframework.http.HttpMethod.GET, getRequest, String.class);
        assertNotNull(pendingResponse.getBody());
        JsonNode pendingBody = objectMapper.readTree(pendingResponse.getBody());
        assertNotNull(pendingBody.get(0).get("dni"));

        ResponseEntity<String> approveResponse = restTemplate.postForEntity("http://localhost:" + port + "/api/admin/users/" + pendingUser.getId() + "/approve", new HttpEntity<>(headers), String.class);
        assertNotNull(approveResponse.getBody());
        JsonNode approveBody = objectMapper.readTree(approveResponse.getBody());
        assertEquals("ACTIVO", approveBody.get("estado").asText());

        User nextPending = createPendingUser();
        ResponseEntity<String> rejectResponse = restTemplate.postForEntity("http://localhost:" + port + "/api/admin/users/" + nextPending.getId() + "/reject", new HttpEntity<>(headers), String.class);
        assertNotNull(rejectResponse.getBody());
        JsonNode rejectBody = objectMapper.readTree(rejectResponse.getBody());
        assertEquals("RECHAZADO", rejectBody.get("estado").asText());
    }

    @Test
    void testDeleteUserWithPasswordViaAdminEndpoint() throws Exception {
        User pendingUser = createPendingUser();
        DeleteUserRequest requestBody = new DeleteUserRequest("admin123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);
        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

        ResponseEntity<Void> response = restTemplate.exchange("http://localhost:" + port + "/api/admin/users/" + pendingUser.getId() + "/with-password", org.springframework.http.HttpMethod.DELETE, request, Void.class);
        assertEquals(204, response.getStatusCode().value());
    }
}
