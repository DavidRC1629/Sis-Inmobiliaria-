package com.sisarovi.inmobiliario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.DevolucionResponse;
import com.sisarovi.inmobiliario.exception.GlobalExceptionHandler;
import com.sisarovi.inmobiliario.service.DevolucionService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DevolucionControllerTest {

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    @Mock DevolucionService devolucionService;
    @InjectMocks DevolucionController devolucionController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(devolucionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private DevolucionResponse sample(Long id, String estado) {
        return DevolucionResponse.builder()
                .id(id).loteId(1L).loteNumero(1)
                .manzana("Manzana A").parcelaNombre("Parcela 1")
                .etapaNumero(1).proyectoNombre("Proyecto")
                .montoTotal(new BigDecimal("1000")).montoPagado(BigDecimal.ZERO)
                .montoPendiente(new BigDecimal("1000"))
                .dias(30).descripcion("Test").estado(estado).progreso(0)
                .pagos(List.of()).build();
    }

    // JSON raw para evitar dependencia de JavaTimeModule
    private static final String BODY_VALID =
            "{\"loteId\":1,\"loteNumero\":1,\"manzana\":\"Manzana A\"," +
            "\"parcelaNombre\":\"Parcela 1\",\"etapaNumero\":1,\"proyectoNombre\":\"Proyecto\"," +
            "\"montoTotal\":1000,\"fechaInicio\":\"2026-06-01\",\"fechaFinEstimada\":\"2026-07-01\"," +
            "\"dias\":30,\"descripcion\":\"Motivo\"}";

    private static final String PAGO_BODY =
            "{\"monto\":300,\"fechaPago\":\"2026-06-01\"," +
            "\"descripcion\":\"Primer pago\",\"medioPago\":\"EFECTIVO\"}";

    // ─── GET /api/devoluciones ───────────────────────────────────────────

    @Test
    void listar_sinFiltro_returns200() throws Exception {
        when(devolucionService.listar(null)).thenReturn(List.of(sample(1L, "EN_CURSO")));

        mockMvc.perform(get("/api/devoluciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("EN_CURSO"));
    }

    @Test
    void listar_conEstado_returns200() throws Exception {
        when(devolucionService.listar("COMPLETADA")).thenReturn(List.of());

        mockMvc.perform(get("/api/devoluciones").param("estado", "COMPLETADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listar_empty_returns200() throws Exception {
        when(devolucionService.listar(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/devoluciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─── GET /api/devoluciones/{id} ──────────────────────────────────────

    @Test
    void obtener_existing_returns200() throws Exception {
        when(devolucionService.obtener(1L)).thenReturn(sample(1L, "EN_CURSO"));

        mockMvc.perform(get("/api/devoluciones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.montoTotal").value(1000));
    }

    @Test
    void obtener_notFound_returns400() throws Exception {
        when(devolucionService.obtener(99L))
                .thenThrow(new IllegalArgumentException("Devolución no encontrada"));

        mockMvc.perform(get("/api/devoluciones/99"))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/devoluciones ──────────────────────────────────────────

    @Test
    void crear_valid_returns200() throws Exception {
        when(devolucionService.crear(any(), any())).thenReturn(sample(2L, "EN_CURSO"));

        mockMvc.perform(post("/api/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALID)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void crear_serviceThrows_returns400() throws Exception {
        when(devolucionService.crear(any(), any()))
                .thenThrow(new RuntimeException("Error al crear devolución"));

        mockMvc.perform(post("/api/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALID)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_sinAuthentication_usaAnonimo() throws Exception {
        when(devolucionService.crear(any(), eq("ANONIMO"))).thenReturn(sample(3L, "EN_CURSO"));

        // Sin .principal() → authentication null → "ANONIMO"
        mockMvc.perform(post("/api/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALID))
                .andExpect(status().isOk());
    }

    // ─── POST /api/devoluciones/{id}/pagos ───────────────────────────────

    @Test
    void registrarPago_valid_returns200() throws Exception {
        when(devolucionService.registrarPago(eq(1L), any(), any()))
                .thenReturn(sample(1L, "EN_CURSO"));

        mockMvc.perform(post("/api/devoluciones/1/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAGO_BODY)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"));
    }

    @Test
    void registrarPago_notFound_returns400() throws Exception {
        when(devolucionService.registrarPago(eq(99L), any(), any()))
                .thenThrow(new IllegalArgumentException("Devolución no encontrada"));

        mockMvc.perform(post("/api/devoluciones/99/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAGO_BODY)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarPago_completaPago_returns200() throws Exception {
        when(devolucionService.registrarPago(eq(1L), any(), any()))
                .thenReturn(sample(1L, "COMPLETADA"));

        String fullPago = "{\"monto\":1000,\"fechaPago\":\"2026-06-01\"," +
                "\"descripcion\":\"Pago final\",\"medioPago\":\"TRANSFERENCIA\"}";

        mockMvc.perform(post("/api/devoluciones/1/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fullPago)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));
    }
}

