package com.sisarovi.inmobiliario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.ProformaRequest;
import com.sisarovi.inmobiliario.dto.ProformaResponse;
import com.sisarovi.inmobiliario.entity.Proforma;
import com.sisarovi.inmobiliario.exception.GlobalExceptionHandler;
import com.sisarovi.inmobiliario.service.ProformaService;
import com.sisarovi.inmobiliario.service.RegistroAuditoriaService;
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
class ProformaControllerTest {

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    @Mock ProformaService proformaService;
    @Mock RegistroAuditoriaService registroAuditoriaService;
    @InjectMocks ProformaController proformaController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(proformaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ProformaResponse sampleResponse() {
        return ProformaResponse.builder()
                .id(1L).codigo("001-ABC").proyecto("Proj")
                .clienteNombre("Juan Perez").clienteDni("12345678").asesor("Asesor")
                .precioContado(new BigDecimal("150000"))
                .hasPdf(false).build();
    }

    private ProformaRequest buildRequest() {
        ProformaRequest req = new ProformaRequest();
        req.setProyecto("Proyecto"); req.setClienteNombre("Juan Perez");
        req.setClienteDni("12345678"); req.setClienteCelular("999888777");
        req.setAsesor("Asesor"); req.setFechaEmision("2026-01-15");
        req.setFechaVencimiento("2026-02-15");
        req.setPrecioContado(new BigDecimal("150000"));
        return req;
    }

    // ─── POST /api/proformas ─────────────────────────────────────────────

    @Test
    void create_valid_returns201() throws Exception {
        when(proformaService.create(any(), any())).thenReturn(sampleResponse());
        doNothing().when(registroAuditoriaService).registrarAccion(any(), any(), any());

        mockMvc.perform(post("/api/proformas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest()))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("001-ABC"));
    }

    @Test
    void create_serviceThrows_returns400() throws Exception {
        when(proformaService.create(any(), any()))
                .thenThrow(new RuntimeException("No se pudo generar código"));

        mockMvc.perform(post("/api/proformas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isBadRequest());
    }

    // ─── GET /api/proformas/historial ────────────────────────────────────

    @Test
    void historial_returns200() throws Exception {
        when(proformaService.historial()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/proformas/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("001-ABC"));
    }

    @Test
    void historial_empty_returns200() throws Exception {
        when(proformaService.historial()).thenReturn(List.of());

        mockMvc.perform(get("/api/proformas/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─── GET /api/proformas/buscar ───────────────────────────────────────

    @Test
    void buscar_poCodigo_returns200() throws Exception {
        when(proformaService.buscar("codigo", "001ABC")).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/proformas/buscar")
                        .param("tipo", "codigo").param("q", "001ABC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("001-ABC"));
    }

    @Test
    void buscar_porNombre_returns200() throws Exception {
        when(proformaService.buscar("nombre", "Juan")).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/proformas/buscar")
                        .param("tipo", "nombre").param("q", "Juan"))
                .andExpect(status().isOk());
    }

    @Test
    void buscar_queryCorta_returnsEmpty() throws Exception {
        when(proformaService.buscar(anyString(), eq("ab"))).thenReturn(List.of());

        mockMvc.perform(get("/api/proformas/buscar").param("q", "ab"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─── GET /api/proformas/{id}/pdf ─────────────────────────────────────

    @Test
    void verPdf_conPdf_returns200() throws Exception {
        Proforma p = buildProforma(1L, "001-ABC", "data".getBytes(), "test.pdf", "application/pdf");
        when(proformaService.getById(1L)).thenReturn(p);

        mockMvc.perform(get("/api/proformas/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"test.pdf\""));
    }

    @Test
    void verPdf_sinPdf_returns404() throws Exception {
        Proforma p = buildProforma(2L, "002-XYZ", null, null, null);
        when(proformaService.getById(2L)).thenReturn(p);

        mockMvc.perform(get("/api/proformas/2/pdf"))
                .andExpect(status().isNotFound());
    }

    @Test
    void verPdf_pdfVacio_returns404() throws Exception {
        Proforma p = buildProforma(3L, "003-ZZZ", new byte[0], null, null);
        when(proformaService.getById(3L)).thenReturn(p);

        mockMvc.perform(get("/api/proformas/3/pdf"))
                .andExpect(status().isNotFound());
    }

    @Test
    void verPdf_sinNombreArchivo_usaCodigoPdf() throws Exception {
        Proforma p = buildProforma(4L, "004-AAA", "data".getBytes(), null, null);
        when(proformaService.getById(4L)).thenReturn(p);

        mockMvc.perform(get("/api/proformas/4/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"004-AAA.pdf\""));
    }

    @Test
    void verPdf_notFound_returns400() throws Exception {
        when(proformaService.getById(99L))
                .thenThrow(new RuntimeException("Proforma no encontrada con ID: 99"));

        mockMvc.perform(get("/api/proformas/99/pdf"))
                .andExpect(status().isBadRequest());
    }

    // ─── helper ──────────────────────────────────────────────────────────

    private Proforma buildProforma(Long id, String codigo, byte[] pdfData,
                                    String fileName, String contentType) {
        Proforma p = new Proforma();
        try {
            var f = Proforma.class.getDeclaredField("id");
            f.setAccessible(true); f.set(p, id);
        } catch (Exception ignored) {}
        p.setCodigo(codigo);
        p.setPdfData(pdfData);
        p.setPdfFileName(fileName);
        p.setPdfContentType(contentType);
        return p;
    }
}

