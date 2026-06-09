package com.sisarovi.inmobiliario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.EtapaRequest;
import com.sisarovi.inmobiliario.dto.EtapaResponse;
import com.sisarovi.inmobiliario.exception.GlobalExceptionHandler;
import com.sisarovi.inmobiliario.service.EtapaService;
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
class EtapaControllerTest {

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    @Mock EtapaService etapaService;
    @InjectMocks EtapaController etapaController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(etapaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private EtapaResponse sample(Long id, int numero) {
        return EtapaResponse.builder().id(id).numeroEtapa(numero).cantidadParcelas(0).projectId(1L).build();
    }

    @Test
    void getEtapasByProject_returns200() throws Exception {
        when(etapaService.getEtapasByProject(1L)).thenReturn(List.of(sample(1L, 1), sample(2L, 2)));

        mockMvc.perform(get("/api/projects/1/etapas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroEtapa").value(1))
                .andExpect(jsonPath("$[1].numeroEtapa").value(2));
    }

    @Test
    void getEtapasByProject_empty_returns200() throws Exception {
        when(etapaService.getEtapasByProject(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/projects/99/etapas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getEtapaById_existing_returns200() throws Exception {
        when(etapaService.getEtapaById(5L)).thenReturn(sample(5L, 3));

        mockMvc.perform(get("/api/projects/1/etapas/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.numeroEtapa").value(3));
    }

    @Test
    void getEtapaById_notFound_returns400() throws Exception {
        when(etapaService.getEtapaById(99L))
                .thenThrow(new RuntimeException("Etapa no encontrada con ID: 99"));

        mockMvc.perform(get("/api/projects/1/etapas/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Etapa no encontrada con ID: 99"));
    }

    @Test
    void createEtapa_valid_returns201() throws Exception {
        when(etapaService.createEtapa(eq(1L), any())).thenReturn(sample(10L, 1));

        mockMvc.perform(post("/api/projects/1/etapas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EtapaRequest(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void createEtapa_duplicate_returns400() throws Exception {
        when(etapaService.createEtapa(eq(1L), any()))
                .thenThrow(new RuntimeException("El número de Etapa ya existe"));

        mockMvc.perform(post("/api/projects/1/etapas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EtapaRequest(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El número de Etapa ya existe"));
    }

    @Test
    void createEtapa_projectNotFound_returns400() throws Exception {
        when(etapaService.createEtapa(eq(99L), any()))
                .thenThrow(new RuntimeException("Proyecto no encontrado con ID: 99"));

        mockMvc.perform(post("/api/projects/99/etapas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EtapaRequest(1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEtapa_valid_returns200() throws Exception {
        when(etapaService.updateEtapa(eq(5L), any())).thenReturn(sample(5L, 2));

        mockMvc.perform(put("/api/projects/1/etapas/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EtapaRequest(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroEtapa").value(2));
    }

    @Test
    void updateEtapa_notFound_returns400() throws Exception {
        when(etapaService.updateEtapa(eq(99L), any()))
                .thenThrow(new RuntimeException("Etapa no encontrada con ID: 99"));

        mockMvc.perform(put("/api/projects/1/etapas/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EtapaRequest(1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteEtapa_existing_returns204() throws Exception {
        doNothing().when(etapaService).deleteEtapa(5L);

        mockMvc.perform(delete("/api/projects/1/etapas/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteEtapa_notFound_returns400() throws Exception {
        doThrow(new RuntimeException("Etapa no encontrada con ID: 99"))
                .when(etapaService).deleteEtapa(99L);

        mockMvc.perform(delete("/api/projects/1/etapas/99"))
                .andExpect(status().isBadRequest());
    }
}
