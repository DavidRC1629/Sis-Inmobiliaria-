package com.sisarovi.inmobiliario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.ParcelaRequest;
import com.sisarovi.inmobiliario.dto.ParcelaResponse;
import com.sisarovi.inmobiliario.exception.GlobalExceptionHandler;
import com.sisarovi.inmobiliario.service.ParcelaService;
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
class ParcelaControllerTest {

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    @Mock ParcelaService parcelaService;
    @InjectMocks ParcelaController parcelaController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(parcelaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ParcelaResponse sample(Long id) {
        return ParcelaResponse.builder()
                .id(id).nombre("Parcela A").numManzanas(2)
                .propietario("Dueño").cantidadLotes(0).lotesDisponibles(0).etapaId(1L).build();
    }

    @Test
    void getParcelaDirectById_existing_returns200() throws Exception {
        when(parcelaService.getParcelaById(1L)).thenReturn(sample(1L));

        mockMvc.perform(get("/api/parcelas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Parcela A"));
    }

    @Test
    void getParcelaDirectById_notFound_returns400() throws Exception {
        when(parcelaService.getParcelaById(99L))
                .thenThrow(new RuntimeException("Parcela no encontrada con ID: 99"));

        mockMvc.perform(get("/api/parcelas/99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getParcelasByEtapa_returns200() throws Exception {
        when(parcelaService.getParcelasByEtapa(1L)).thenReturn(List.of(sample(1L), sample(2L)));

        mockMvc.perform(get("/api/etapas/1/parcelas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Parcela A"));
    }

    @Test
    void getParcelasByEtapa_empty_returns200() throws Exception {
        when(parcelaService.getParcelasByEtapa(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/etapas/99/parcelas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getParcelaById_viaEtapa_returns200() throws Exception {
        when(parcelaService.getParcelaById(5L)).thenReturn(sample(5L));

        mockMvc.perform(get("/api/etapas/1/parcelas/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void searchByPropietario_returns200() throws Exception {
        when(parcelaService.searchByPropietario("Dueño")).thenReturn(List.of(sample(1L)));

        mockMvc.perform(get("/api/etapas/1/parcelas/search").param("propietario", "Dueño"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].propietario").value("Dueño"));
    }

    @Test
    void createParcela_valid_returns201() throws Exception {
        when(parcelaService.createParcela(eq(1L), any())).thenReturn(sample(10L));

        mockMvc.perform(post("/api/etapas/1/parcelas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParcelaRequest("Nueva", 2, "Owner"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void createParcela_etapaNotFound_returns400() throws Exception {
        when(parcelaService.createParcela(eq(99L), any()))
                .thenThrow(new RuntimeException("Etapa no encontrada con ID: 99"));

        mockMvc.perform(post("/api/etapas/99/parcelas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParcelaRequest("X", 1, "Y"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateParcela_valid_returns200() throws Exception {
        when(parcelaService.updateParcela(eq(1L), any())).thenReturn(sample(1L));

        mockMvc.perform(put("/api/etapas/1/parcelas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParcelaRequest("Actualizada", 1, "NuevoDueño"))))
                .andExpect(status().isOk());
    }

    @Test
    void updateParcela_notFound_returns400() throws Exception {
        when(parcelaService.updateParcela(eq(99L), any()))
                .thenThrow(new RuntimeException("Parcela no encontrada con ID: 99"));

        mockMvc.perform(put("/api/etapas/1/parcelas/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParcelaRequest("X", 1, "Y"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteParcela_existing_returns204() throws Exception {
        doNothing().when(parcelaService).deleteParcela(1L);

        mockMvc.perform(delete("/api/etapas/1/parcelas/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteParcela_notFound_returns400() throws Exception {
        doThrow(new RuntimeException("Parcela no encontrada con ID: 99"))
                .when(parcelaService).deleteParcela(99L);

        mockMvc.perform(delete("/api/etapas/1/parcelas/99"))
                .andExpect(status().isBadRequest());
    }
}
