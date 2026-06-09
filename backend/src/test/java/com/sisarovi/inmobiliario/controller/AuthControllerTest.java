package com.sisarovi.inmobiliario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.AuthResponse;
import com.sisarovi.inmobiliario.dto.LoginRequest;
import com.sisarovi.inmobiliario.dto.RegisterRequest;
import com.sisarovi.inmobiliario.exception.GlobalExceptionHandler;
import com.sisarovi.inmobiliario.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    @Mock AuthService authService;
    @InjectMocks AuthController authController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AuthResponse sampleAuthResponse() {
        return AuthResponse.builder()
                .token("jwt-token").dni("12345678")
                .nombres("Juan").primerApellido("Perez")
                .role("ROLE_USER").build();
    }

    // ─── POST /api/auth/register ─────────────────────────────────────────

    @Test
    void register_validRequest_returns200() throws Exception {
        when(authService.register(any())).thenReturn(sampleAuthResponse());

        RegisterRequest req = RegisterRequest.builder()
                .dni("12345678").email("j@test.com").password("pass")
                .nombres("Juan").primerApellido("Perez").segundoApellido("X").build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.dni").value("12345678"));
    }

    @Test
    void register_serviceThrows_returns400() throws Exception {
        when(authService.register(any())).thenThrow(new RuntimeException("El DNI ya está registrado"));

        RegisterRequest req = RegisterRequest.builder()
                .dni("12345678").email("j@test.com").password("pass")
                .nombres("Juan").primerApellido("Perez").segundoApellido("X").build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El DNI ya está registrado"));
    }

    // ─── POST /api/auth/login ────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200() throws Exception {
        when(authService.login(any())).thenReturn(sampleAuthResponse());

        LoginRequest req = LoginRequest.builder().identifier("12345678").password("pass").build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("bad"));

        LoginRequest req = LoginRequest.builder().identifier("bad").password("wrong").build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_accountDisabled_returns403() throws Exception {
        when(authService.login(any()))
                .thenThrow(new DisabledException("disabled"));

        LoginRequest req = LoginRequest.builder().identifier("12345678").password("pass").build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_serviceThrows_returns400() throws Exception {
        when(authService.login(any()))
                .thenThrow(new RuntimeException("Debe ingresar DNI o correo"));

        LoginRequest req = LoginRequest.builder().identifier("").password("pass").build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debe ingresar DNI o correo"));
    }

    // ─── POST /api/auth/forgot-password/request ──────────────────────────

    @Test
    void requestPasswordRecovery_validEmail_returns200() throws Exception {
        when(authService.requestPasswordRecovery(anyString()))
                .thenReturn(Map.of("message", "Código enviado"));

        mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"j@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Código enviado"));
    }

    @Test
    void requestPasswordRecovery_nullEmail_returns400() throws Exception {
        when(authService.requestPasswordRecovery(null))
                .thenThrow(new RuntimeException("Debe ingresar un correo electrónico"));

        mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/auth/forgot-password/confirm ──────────────────────────

    @Test
    void confirmPasswordRecovery_valid_returns200() throws Exception {
        when(authService.confirmPasswordRecovery(anyString(), anyString(), anyString()))
                .thenReturn(Map.of("message", "Contraseña actualizada"));

        String body = "{\"email\":\"j@test.com\",\"temporaryCode\":\"ABCD1234\",\"newPassword\":\"new123\"}";

        mockMvc.perform(post("/api/auth/forgot-password/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contraseña actualizada"));
    }

    @Test
    void confirmPasswordRecovery_expiredCode_returns400() throws Exception {
        when(authService.confirmPasswordRecovery(any(), any(), any()))
                .thenThrow(new RuntimeException("El código temporal expiró"));

        String body = "{\"email\":\"j@test.com\",\"temporaryCode\":\"EXPIRED1\",\"newPassword\":\"p\"}";

        mockMvc.perform(post("/api/auth/forgot-password/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
