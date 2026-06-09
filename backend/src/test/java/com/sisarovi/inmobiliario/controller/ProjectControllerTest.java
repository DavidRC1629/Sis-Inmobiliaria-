package com.sisarovi.inmobiliario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.ProjectRequest;
import com.sisarovi.inmobiliario.dto.ProjectResponse;
import com.sisarovi.inmobiliario.exception.GlobalExceptionHandler;
import com.sisarovi.inmobiliario.service.ProjectService;
import com.sisarovi.inmobiliario.service.RegistroAuditoriaService;
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
class ProjectControllerTest {

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    @Mock ProjectService projectService;
    @Mock RegistroAuditoriaService registroAuditoriaService;
    @InjectMocks ProjectController projectController;

    private static final UsernamePasswordAuthenticationToken ADMIN_AUTH =
            new UsernamePasswordAuthenticationToken("admin", null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    /** Resuelve el parámetro Authentication en standaloneSetup */
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
        mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(AUTH_RESOLVER)
                .build();
    }

    private ProjectResponse sample(Long id) {
        return ProjectResponse.builder()
                .id(id).nombre("Proyecto Alpha").cantidadEtapas(2)
                .cantidadParcelasTotal(0).createdByNombre("Admin").build();
    }

    // ─── GET /api/projects ───────────────────────────────────────────────

    @Test
    void getAllProjects_returns200() throws Exception {
        when(projectService.getAllProjects()).thenReturn(List.of(sample(1L)));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Proyecto Alpha"))
                .andExpect(jsonPath("$[0].cantidadEtapas").value(2));
    }

    @Test
    void getAllProjects_empty_returns200() throws Exception {
        when(projectService.getAllProjects()).thenReturn(List.of());

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─── GET /api/projects/{id} ──────────────────────────────────────────

    @Test
    void getProjectById_existing_returns200() throws Exception {
        when(projectService.getProjectById(1L)).thenReturn(sample(1L));

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getProjectById_notFound_returns400() throws Exception {
        when(projectService.getProjectById(99L))
                .thenThrow(new RuntimeException("Proyecto no encontrado con ID: 99"));

        mockMvc.perform(get("/api/projects/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Proyecto no encontrado con ID: 99"));
    }

    // ─── POST /api/projects ──────────────────────────────────────────────

    @Test
    void createProject_valid_returns201() throws Exception {
        // El service se llama con any() porque authentication.getName() no se propaga
        // en standaloneSetup — pero el controller llega al service y devuelve la respuesta
        when(projectService.createProject(any(), any())).thenReturn(sample(2L));
        doNothing().when(registroAuditoriaService).registrarAccion(any(), any(), any());

        ProjectRequest req = new ProjectRequest("Nuevo Proyecto", null, null, 3);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(ADMIN_AUTH))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Proyecto Alpha"));
    }

    @Test
    void createProject_duplicateName_returns400() throws Exception {
        when(projectService.createProject(any(), any()))
                .thenThrow(new RuntimeException("Ya existe un proyecto con ese nombre"));

        ProjectRequest req = new ProjectRequest("Duplicado", null, null, 1);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .principal(ADMIN_AUTH))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ya existe un proyecto con ese nombre"));
    }

    // ─── PUT /api/projects/{id} ──────────────────────────────────────────

    @Test
    void updateProject_valid_returns200() throws Exception {
        ProjectResponse before = sample(1L);
        when(projectService.getProjectById(1L)).thenReturn(before);
        when(projectService.updateProject(eq(1L), any())).thenReturn(sample(1L));
        doNothing().when(registroAuditoriaService).registrarAccion(any(), any(), any());

        ProjectRequest req = new ProjectRequest("Actualizado", null, null, 2);

        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void updateProject_notFound_returns400() throws Exception {
        when(projectService.getProjectById(99L))
                .thenThrow(new RuntimeException("Proyecto no encontrado con ID: 99"));

        ProjectRequest req = new ProjectRequest("X", null, null, 1);

        mockMvc.perform(put("/api/projects/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ─── DELETE /api/projects/{id} ───────────────────────────────────────

    @Test
    void deleteProject_existing_returns204() throws Exception {
        when(projectService.getProjectById(1L)).thenReturn(sample(1L));
        doNothing().when(projectService).deleteProject(1L);
        doNothing().when(registroAuditoriaService).registrarAccion(any(), any(), any());

        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProject_withClientes_returns400() throws Exception {
        when(projectService.getProjectById(1L)).thenReturn(sample(1L));
        doThrow(new RuntimeException("No se puede eliminar este proyecto"))
                .when(projectService).deleteProject(1L);

        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isBadRequest());
    }

    // ─── PUT /api/projects/{id}/logo ─────────────────────────────────────

    @Test
    void updateProjectLogo_withLogoUrl_returns200() throws Exception {
        when(projectService.updateProjectLogo(eq(1L), eq("http://logo.png"))).thenReturn(sample(1L));
        doNothing().when(registroAuditoriaService).registrarAccion(any(), any(), any());

        mockMvc.perform(put("/api/projects/1/logo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logoUrl\":\"http://logo.png\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProjectLogo_emptyLogo_usesElimino() throws Exception {
        when(projectService.updateProjectLogo(eq(1L), eq(""))).thenReturn(sample(1L));
        doNothing().when(registroAuditoriaService).registrarAccion(any(), any(), any());

        mockMvc.perform(put("/api/projects/1/logo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logoUrl\":\"\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProjectLogo_missingKey_usesDefault() throws Exception {
        when(projectService.updateProjectLogo(eq(1L), eq(""))).thenReturn(sample(1L));
        doNothing().when(registroAuditoriaService).registrarAccion(any(), any(), any());

        mockMvc.perform(put("/api/projects/1/logo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}



