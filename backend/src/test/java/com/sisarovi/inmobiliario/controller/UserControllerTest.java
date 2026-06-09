package com.sisarovi.inmobiliario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.ChangeRoleRequest;
import com.sisarovi.inmobiliario.dto.UserResponse;
import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.exception.GlobalExceptionHandler;
import com.sisarovi.inmobiliario.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    @Mock UserService userService;
    @InjectMocks UserController userController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UserResponse sample(Long id) {
        return UserResponse.builder()
                .id(id).dni("12345678").nombres("Juan")
                .primerApellido("Perez").segundoApellido("Lopez")
                .role("ROLE_USER").estado(UserStatus.ACTIVO.name()).enabled(true).build();
    }

    @Test
    void getAllUsers_returns200() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(sample(1L), sample(2L)));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dni").value("12345678"));
    }

    @Test
    void getAllUsers_empty_returns200() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getPendingUsers_returns200() throws Exception {
        UserResponse pending = UserResponse.builder()
                .id(3L).dni("87654321").nombres("Ana").primerApellido("T")
                .role("ROLE_USER").estado(UserStatus.PENDIENTE.name()).enabled(true).build();
        when(userService.getPendingUsers()).thenReturn(List.of(pending));

        mockMvc.perform(get("/api/users/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }

    @Test
    void getPendingUsers_noPending_returns200() throws Exception {
        when(userService.getPendingUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/users/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getUserById_existing_returns200() throws Exception {
        when(userService.getUserById(1L)).thenReturn(sample(1L));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dni").value("12345678"));
    }

    @Test
    void getUserById_notFound_returns400() throws Exception {
        when(userService.getUserById(99L))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
    }

    @Test
    void changeUserRole_valid_returns200() throws Exception {
        UserResponse updated = UserResponse.builder()
                .id(1L).dni("12345678").nombres("Juan").primerApellido("P")
                .role("ROLE_ADMIN").estado(UserStatus.ACTIVO.name()).enabled(true).build();
        when(userService.changeUserRole(any())).thenReturn(updated);

        ChangeRoleRequest req = new ChangeRoleRequest();
        req.setUserId(1L);
        req.setNewRole("ROLE_ADMIN");

        mockMvc.perform(put("/api/users/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    void changeUserRole_userNotFound_returns400() throws Exception {
        when(userService.changeUserRole(any()))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        ChangeRoleRequest req = new ChangeRoleRequest();
        req.setUserId(99L);
        req.setNewRole("ROLE_ADMIN");

        mockMvc.perform(put("/api/users/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeUserRole_roleNotFound_returns400() throws Exception {
        when(userService.changeUserRole(any()))
                .thenThrow(new RuntimeException("Rol no encontrado: ROLE_GHOST"));

        ChangeRoleRequest req = new ChangeRoleRequest();
        req.setUserId(1L);
        req.setNewRole("ROLE_GHOST");

        mockMvc.perform(put("/api/users/change-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rol no encontrado: ROLE_GHOST"));
    }
}
