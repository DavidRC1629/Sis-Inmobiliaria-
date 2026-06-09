package com.sisarovi.inmobiliario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.LoteRequest;
import com.sisarovi.inmobiliario.dto.LoteResponse;
import com.sisarovi.inmobiliario.exception.GlobalExceptionHandler;
import com.sisarovi.inmobiliario.service.LoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LoteControllerTest {

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    @Mock LoteService loteService;
    @InjectMocks LoteController loteController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(loteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private LoteResponse sampleLote(Long id) {
        return LoteResponse.builder()
                .id(id).numero(1).numeroPartida("12345678")
                .precioLote(new BigDecimal("50000"))
                .parcelaId(1L).parcelaNombre("Parcela A")
                .etapaNumero(1).projectId(1L).projectNombre("Proyecto")
                .adquirido(false).build();
    }

    private LoteRequest validRequest() {
        LoteRequest req = new LoteRequest();
        req.setNumero(1);
        req.setNumeroPartida("12345678");
        req.setPrecioLote(new BigDecimal("50000"));
        req.setManzanaId(1L);
        return req;
    }

    @Test
    void getLotesByParcela_returns200() throws Exception {
        when(loteService.getLotesByParcela(1L)).thenReturn(List.of(sampleLote(1L)));

        mockMvc.perform(get("/api/parcelas/1/lotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value(1));
    }

    @Test
    void getLotesByParcela_empty_returns200() throws Exception {
        when(loteService.getLotesByParcela(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/parcelas/99/lotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getLotesByManzana_valid_returns200() throws Exception {
        when(loteService.getLotesByParcelaAndManzana(1L, "1")).thenReturn(List.of(sampleLote(1L)));

        mockMvc.perform(get("/api/parcelas/1/lotes/manzana/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getLotesByManzana_invalidId_returns400() throws Exception {
        when(loteService.getLotesByParcelaAndManzana(1L, "abc"))
                .thenThrow(new RuntimeException("El id de manzana debe ser un número válido"));

        mockMvc.perform(get("/api/parcelas/1/lotes/manzana/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getLoteById_existing_returns200() throws Exception {
        when(loteService.getLoteById(10L)).thenReturn(sampleLote(10L));

        mockMvc.perform(get("/api/parcelas/1/lotes/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void getLoteById_notFound_returns400() throws Exception {
        when(loteService.getLoteById(99L))
                .thenThrow(new RuntimeException("Lote no encontrado con ID: 99"));

        mockMvc.perform(get("/api/parcelas/1/lotes/99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void existsNumeroPartida_found_returnsTrue() throws Exception {
        when(loteService.existsNumeroPartidaGlobal("12345678", null)).thenReturn(true);

        mockMvc.perform(get("/api/parcelas/1/lotes/numero-partida/existe")
                        .param("numeroPartida", "12345678"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void existsNumeroPartida_withExclude_returnsFalse() throws Exception {
        when(loteService.existsNumeroPartidaGlobal("12345678", 10L)).thenReturn(false);

        mockMvc.perform(get("/api/parcelas/1/lotes/numero-partida/existe")
                        .param("numeroPartida", "12345678")
                        .param("excludeLoteId", "10"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void createLote_valid_returns201() throws Exception {
        when(loteService.createLote(eq(1L), any())).thenReturn(sampleLote(20L));

        mockMvc.perform(post("/api/parcelas/1/lotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20));
    }

    @Test
    void createLote_duplicatePartida_returns400() throws Exception {
        when(loteService.createLote(eq(1L), any()))
                .thenThrow(new RuntimeException("El número de partida 12345678 ya existe"));

        mockMvc.perform(post("/api/parcelas/1/lotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateLote_valid_returns200() throws Exception {
        when(loteService.updateLote(eq(10L), any())).thenReturn(sampleLote(10L));

        mockMvc.perform(put("/api/parcelas/1/lotes/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void updateLote_notFound_returns400() throws Exception {
        when(loteService.updateLote(eq(99L), any()))
                .thenThrow(new RuntimeException("Lote no encontrado con ID: 99"));

        mockMvc.perform(put("/api/parcelas/1/lotes/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteLote_existing_returns204() throws Exception {
        doNothing().when(loteService).deleteLote(10L);

        mockMvc.perform(delete("/api/parcelas/1/lotes/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteLote_notFound_returns400() throws Exception {
        doThrow(new RuntimeException("Lote no encontrado con ID: 99"))
                .when(loteService).deleteLote(99L);

        mockMvc.perform(delete("/api/parcelas/1/lotes/99"))
                .andExpect(status().isBadRequest());
    }
}
