package com.sisarovi.inmobiliario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.TerrenoPropioResponse;
import com.sisarovi.inmobiliario.exception.GlobalExceptionHandler;
import com.sisarovi.inmobiliario.service.RegistroAuditoriaService;
import com.sisarovi.inmobiliario.service.TerrenoPropioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TerrenoPropioControllerTest {

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    @Mock TerrenoPropioService terrenoPropioService;
    @Mock RegistroAuditoriaService registroAuditoriaService;
    @InjectMocks TerrenoPropioController terrenoPropioController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(terrenoPropioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        doNothing().when(registroAuditoriaService).registrarAccion(any(), any(), any());
    }

    private TerrenoPropioResponse sample(Long id) {
        return new TerrenoPropioResponse(
                id, 5, "Av. Principal", new BigDecimal("120"), new BigDecimal("44"),
                new BigDecimal("10"), new BigDecimal("12"), new BigDecimal("11"),
                new BigDecimal("11"), "12345678", new BigDecimal("50000"),
                null, null, "DISPONIBLE");
    }

    // ─── GET /api/terrenos-propios ────────────────────────────────────────────

    @Test
    void getAll_returns200() throws Exception {
        when(terrenoPropioService.getAll()).thenReturn(List.of(sample(1L)));

        mockMvc.perform(get("/api/terrenos-propios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("DISPONIBLE"));
    }

    @Test
    void getAll_empty_returns200() throws Exception {
        when(terrenoPropioService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/terrenos-propios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─── GET /api/terrenos-propios/{id} ──────────────────────────────────────

    @Test
    void getById_existing_returns200() throws Exception {
        when(terrenoPropioService.getById(1L)).thenReturn(Optional.of(sample(1L)));

        mockMvc.perform(get("/api/terrenos-propios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(terrenoPropioService.getById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/terrenos-propios/99"))
                .andExpect(status().isNotFound());
    }

    // ─── GET /api/terrenos-propios/exists/partida/{partida} ──────────────────

    @Test
    void existsByNumeroPartida_true_returns200() throws Exception {
        when(terrenoPropioService.existsByNumeroPartida("12345678")).thenReturn(true);

        mockMvc.perform(get("/api/terrenos-propios/exists/partida/12345678"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void existsByNumeroPartida_false_returns200() throws Exception {
        when(terrenoPropioService.existsByNumeroPartida("99999999")).thenReturn(false);

        mockMvc.perform(get("/api/terrenos-propios/exists/partida/99999999"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    // ─── POST /api/terrenos-propios ───────────────────────────────────────────

    @Test
    void create_valid_returns201() throws Exception {
        when(terrenoPropioService.create(any())).thenReturn(sample(2L));

        String body = "{\"numeroLote\":5,\"calle\":\"Av. Test\",\"areaM2\":120," +
                "\"perimetro\":44,\"medidaFrente\":10,\"medidaFondo\":12," +
                "\"medidaIzquierda\":11,\"medidaDerecha\":11," +
                "\"numeroPartida\":\"87654321\",\"precio\":50000,\"propietarioId\":1}";

        mockMvc.perform(post("/api/terrenos-propios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
    }

    @Test
    void create_duplicatePartida_returns400() throws Exception {
        when(terrenoPropioService.create(any()))
                .thenThrow(new IllegalArgumentException("El número de partida ya existe en el sistema"));

        String body = "{\"numeroLote\":5,\"calle\":\"Av.\",\"areaM2\":120," +
                "\"perimetro\":44,\"medidaFrente\":10,\"medidaFondo\":12," +
                "\"medidaIzquierda\":11,\"medidaDerecha\":11," +
                "\"numeroPartida\":\"12345678\",\"precio\":50000,\"propietarioId\":1}";

        mockMvc.perform(post("/api/terrenos-propios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ─── DELETE /api/terrenos-propios/{id}/imagen ─────────────────────────────

    @Test
    void deleteImagen_existing_returns204() throws Exception {
        doNothing().when(terrenoPropioService).deleteImagen(1L);

        mockMvc.perform(delete("/api/terrenos-propios/1/imagen"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteImagen_notFound_returns400() throws Exception {
        doThrow(new IllegalArgumentException("Terreno propio no encontrado"))
                .when(terrenoPropioService).deleteImagen(99L);

        mockMvc.perform(delete("/api/terrenos-propios/99/imagen"))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/terrenos-propios/{id}/adquirir ─────────────────────────────

    @Test
    void adquirirTerreno_valid_returns200() throws Exception {
        doNothing().when(terrenoPropioService).adquirirTerreno(eq(1L), any(), any(), anyInt(), anyDouble());

        String body = "{\"clienteId\":1,\"formaPago\":\"CONTADO\",\"cuotas\":1,\"interes\":0}";

        mockMvc.perform(post("/api/terrenos-propios/1/adquirir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void adquirirTerreno_noDisponible_returns400() throws Exception {
        doThrow(new IllegalStateException("El terreno no está disponible para adquisición"))
                .when(terrenoPropioService).adquirirTerreno(eq(1L), any(), any(), anyInt(), anyDouble());

        String body = "{\"clienteId\":1,\"formaPago\":\"CONTADO\",\"cuotas\":1,\"interes\":0}";

        mockMvc.perform(post("/api/terrenos-propios/1/adquirir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
