package com.sisarovi.inmobiliario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.DeleteUserRequest;
import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.exception.GlobalExceptionHandler;
import com.sisarovi.inmobiliario.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    @Mock AdminService adminService;
    @InjectMocks AdminController adminController;

    private static final UsernamePasswordAuthenticationToken ADMIN_AUTH =
            new UsernamePasswordAuthenticationToken("admin", null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    /** Inyecta Authentication en parámetros del controller con standaloneSetup */
    private static final HandlerMethodArgumentResolver AUTH_RESOLVER = new HandlerMethodArgumentResolver() {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return Authentication.class.isAssignableFrom(parameter.getParameterType());
        }
        @Override
        public Object resolveArgument(MethodParameter p, ModelAndViewContainer m,
                                      NativeWebRequest r, WebDataBinderFactory f) {
            return ADMIN_AUTH;
        }
    };

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(AUTH_RESOLVER)
                .build();
    }

    private User sampleUser(Long id, UserStatus estado) {
        Role role = Role.builder().name("ROLE_USER").build();
        setField(role, "id", 1L);
        User u = User.builder()
                .dni("12345678").nombres("Juan").primerApellido("P").segundoApellido("")
                .password("pw").email("j@t.com").role(role).estado(estado).enabled(true).build();
        setField(u, "id", id);
        return u;
    }

    // ─── GET /api/admin/users/pending ────────────────────────────────────

    @Test
    void getPendingUsers_returns200() throws Exception {
        when(adminService.getPendingUsers()).thenReturn(List.of(sampleUser(1L, UserStatus.PENDIENTE)));

        mockMvc.perform(get("/api/admin/users/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }

    @Test
    void getPendingUsers_empty_returns200() throws Exception {
        when(adminService.getPendingUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/users/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─── GET /api/admin/users/waiting ────────────────────────────────────

    @Test
    void getRejectedWaiting_returns200() throws Exception {
        when(adminService.getRejectedWaitingUsers()).thenReturn(List.of(sampleUser(2L, UserStatus.RECHAZADO)));

        mockMvc.perform(get("/api/admin/users/waiting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("RECHAZADO"));
    }

    // ─── GET /api/admin/users ────────────────────────────────────────────

    @Test
    void getAllUsers_returns200() throws Exception {
        when(adminService.getAllUsers()).thenReturn(List.of(sampleUser(1L, UserStatus.ACTIVO)));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dni").value("12345678"));
    }

    // ─── POST /api/admin/users/{id}/approve ──────────────────────────────

    @Test
    void approveUser_valid_returns200() throws Exception {
        when(adminService.approveUser(1L)).thenReturn(sampleUser(1L, UserStatus.ACTIVO));

        mockMvc.perform(post("/api/admin/users/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void approveUser_notFound_returns400() throws Exception {
        when(adminService.approveUser(99L))
                .thenThrow(new RuntimeException("Usuario no encontrado con id: 99"));

        mockMvc.perform(post("/api/admin/users/99/approve"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveUser_notPending_returns400() throws Exception {
        when(adminService.approveUser(1L))
                .thenThrow(new RuntimeException("El usuario no está en estado pendiente"));

        mockMvc.perform(post("/api/admin/users/1/approve"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El usuario no está en estado pendiente"));
    }

    // ─── POST /api/admin/users/{id}/reject ───────────────────────────────

    @Test
    void rejectUser_valid_returns200() throws Exception {
        when(adminService.rejectUser(1L)).thenReturn(sampleUser(1L, UserStatus.RECHAZADO));

        mockMvc.perform(post("/api/admin/users/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"));
    }

    @Test
    void rejectUser_notPending_returns400() throws Exception {
        when(adminService.rejectUser(1L))
                .thenThrow(new RuntimeException("El usuario no está en estado pendiente"));

        mockMvc.perform(post("/api/admin/users/1/reject"))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/admin/users/{id}/cancel-wait ──────────────────────────

    @Test
    void cancelRejectionCooldown_valid_returns200() throws Exception {
        when(adminService.cancelRejectionCooldown(1L)).thenReturn(sampleUser(1L, UserStatus.RECHAZADO));

        mockMvc.perform(post("/api/admin/users/1/cancel-wait"))
                .andExpect(status().isOk());
    }

    @Test
    void cancelRejectionCooldown_notRejected_returns400() throws Exception {
        when(adminService.cancelRejectionCooldown(1L))
                .thenThrow(new RuntimeException("El usuario no está rechazado"));

        mockMvc.perform(post("/api/admin/users/1/cancel-wait"))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/admin/users/{id}/promote ──────────────────────────────

    @Test
    void promoteToAdmin_valid_returns200() throws Exception {
        Role adminRole = Role.builder().name("ROLE_ADMIN").build();
        setField(adminRole, "id", 2L);
        User promoted = sampleUser(1L, UserStatus.ACTIVO);
        promoted.setRole(adminRole);
        when(adminService.promoteToAdmin(1L)).thenReturn(promoted);

        mockMvc.perform(post("/api/admin/users/1/promote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role.name").value("ROLE_ADMIN"));
    }

    @Test
    void promoteToAdmin_notFound_returns400() throws Exception {
        when(adminService.promoteToAdmin(99L))
                .thenThrow(new RuntimeException("Usuario no encontrado con id: 99"));

        mockMvc.perform(post("/api/admin/users/99/promote"))
                .andExpect(status().isBadRequest());
    }

    // ─── DELETE /api/admin/users/{id}/with-password ──────────────────────

    @Test
    void deleteUserWithPassword_valid_returns204() throws Exception {
        doNothing().when(adminService).deleteUserWithPassword(eq(1L), eq("admin"), eq("admin123"));

        DeleteUserRequest req = new DeleteUserRequest("admin123");

        // Pasar ADMIN_AUTH directamente como Principal — Spring lo trata como Authentication
        mockMvc.perform(delete("/api/admin/users/1/with-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(ADMIN_AUTH))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUserWithPassword_wrongPassword_returns400() throws Exception {
        doThrow(new RuntimeException("Contraseña de administrador incorrecta"))
                .when(adminService).deleteUserWithPassword(eq(1L), eq("admin"), eq("wrong"));

        DeleteUserRequest req = new DeleteUserRequest("wrong");

        mockMvc.perform(delete("/api/admin/users/1/with-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(ADMIN_AUTH))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Contraseña de administrador incorrecta"));
    }

    // ─── helper ──────────────────────────────────────────────────────────

    private void setField(Object obj, String name, Object value) {
        try {
            var f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception ignored) {}
    }
}
