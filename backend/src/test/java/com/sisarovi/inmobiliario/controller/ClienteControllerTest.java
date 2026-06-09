package com.sisarovi.inmobiliario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.*;
import com.sisarovi.inmobiliario.exception.GlobalExceptionHandler;
import com.sisarovi.inmobiliario.service.ClienteService;
import com.sisarovi.inmobiliario.service.RegistroAuditoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClienteControllerTest {

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    @Mock ClienteService clienteService;
    @Mock RegistroAuditoriaService registroAuditoriaService;
    @InjectMocks ClienteController clienteController;

    private static final UsernamePasswordAuthenticationToken USER_AUTH =
            new UsernamePasswordAuthenticationToken("admin", null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    private static final HandlerMethodArgumentResolver AUTH_RESOLVER = new HandlerMethodArgumentResolver() {
        @Override public boolean supportsParameter(MethodParameter p) {
            return Authentication.class.isAssignableFrom(p.getParameterType());
        }
        @Override public Object resolveArgument(MethodParameter p, ModelAndViewContainer m,
                                                NativeWebRequest r, WebDataBinderFactory f) {
            return USER_AUTH;
        }
    };

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(AUTH_RESOLVER)
                .build();
        doNothing().when(registroAuditoriaService).registrarAccion(any(), any(), any());
        doNothing().when(registroAuditoriaService).registrarAccion(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private ClienteResponse sampleCliente(Long id) {
        return ClienteResponse.builder()
                .id(id).nombres("Juan").apellidos("Perez").dni("12345678")
                .email("j@t.com").telefono("999").direccion("Av.")
                .tipoRelacion("ADQUISICION").clienteDesde(LocalDate.now())
                .loteId(1L).loteNumero(1).manzana("A")
                .parcelaNombre("Parcela 1").etapaNumero(1)
                .projectId(1L).projectNombre("Proyecto").build();
    }

    // ─── GET /api/clientes ───────────────────────────────────────────────

    @Test
    void getAllClientes_noQuery_returns200() throws Exception {
        when(clienteService.getAllClientes(null)).thenReturn(List.of(sampleCliente(1L)));

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombres").value("Juan"));
    }

    @Test
    void getAllClientes_withQuery_returns200() throws Exception {
        when(clienteService.getAllClientes("juan")).thenReturn(List.of(sampleCliente(1L)));

        mockMvc.perform(get("/api/clientes").param("q", "juan"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllClientes_empty_returns200() throws Exception {
        when(clienteService.getAllClientes(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─── GET /api/clientes/{id} ──────────────────────────────────────────

    @Test
    void getClienteById_existing_returns200() throws Exception {
        when(clienteService.getClienteById(1L)).thenReturn(sampleCliente(1L));

        mockMvc.perform(get("/api/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getClienteById_notFound_returns400() throws Exception {
        when(clienteService.getClienteById(99L))
                .thenThrow(new RuntimeException("Cliente no encontrado"));

        mockMvc.perform(get("/api/clientes/99"))
                .andExpect(status().isBadRequest());
    }

    // ─── GET /api/clientes/proyectos ─────────────────────────────────────

    @Test
    void getProjectSummaries_returns200() throws Exception {
        when(clienteService.getProjectSummaries()).thenReturn(List.of(
                ClienteProjectSummaryResponse.builder().projectId(1L).projectNombre("P").cantidadClientes(2).build()
        ));

        mockMvc.perform(get("/api/clientes/proyectos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cantidadClientes").value(2));
    }

    // ─── GET /api/clientes/proyectos/{id} ────────────────────────────────

    @Test
    void getClientesByProject_returns200() throws Exception {
        when(clienteService.getClientesByProject(1L)).thenReturn(List.of(sampleCliente(1L)));

        mockMvc.perform(get("/api/clientes/proyectos/1"))
                .andExpect(status().isOk());
    }

    // ─── GET /api/clientes/lotes-disponibles ─────────────────────────────

    @Test
    void getLotesDisponibles_returns200() throws Exception {
        when(clienteService.getLotesDisponibles(1L, null)).thenReturn(List.of(
                ClienteLoteOptionResponse.builder().loteId(1L).loteNumero(1).manzana("A")
                        .parcelaNombre("P1").etapaNumero(1).projectId(1L).projectNombre("Proj").build()
        ));

        mockMvc.perform(get("/api/clientes/lotes-disponibles").param("projectId", "1"))
                .andExpect(status().isOk());
    }

    // ─── POST /api/clientes ──────────────────────────────────────────────

    @Test
    void createCliente_valid_returns201() throws Exception {
        ClienteResponse created = sampleCliente(2L);
        when(clienteService.createCliente(any())).thenReturn(created);

        String body = "{\"nombres\":\"Juan\",\"apellidos\":\"Perez\",\"dni\":\"12345678\"," +
                "\"email\":\"j@t.com\",\"telefono\":\"999\",\"direccion\":\"Av.\"," +
                "\"loteId\":1,\"tipoRelacion\":\"ADQUISICION\",\"clienteDesde\":\"2026-01-01\"}";

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombres").value("Juan"));
    }

    @Test
    void createCliente_loteAdquirido_returns400() throws Exception {
        when(clienteService.createCliente(any()))
                .thenThrow(new RuntimeException("Este lote ya está adquirido"));

        String body = "{\"nombres\":\"Juan\",\"apellidos\":\"Perez\",\"dni\":\"12345678\"," +
                "\"email\":\"j@t.com\",\"telefono\":\"999\",\"direccion\":\"Av.\"," +
                "\"loteId\":1,\"tipoRelacion\":\"ADQUISICION\",\"clienteDesde\":\"2026-01-01\"}";

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ─── PUT /api/clientes/{id} ──────────────────────────────────────────

    @Test
    void updateCliente_sinCambios_returns200() throws Exception {
        ClienteResponse before = sampleCliente(1L);
        ClienteResponse after = sampleCliente(1L);
        when(clienteService.getClienteById(1L)).thenReturn(before);
        when(clienteService.updateCliente(eq(1L), any())).thenReturn(after);

        String body = "{\"nombres\":\"Juan\",\"apellidos\":\"Perez\",\"dni\":\"12345678\"," +
                "\"email\":\"j@t.com\",\"telefono\":\"999\",\"direccion\":\"Av.\"," +
                "\"loteId\":1,\"tipoRelacion\":\"ADQUISICION\",\"clienteDesde\":\"2026-01-01\"}";

        mockMvc.perform(put("/api/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void updateCliente_conCambios_returns200() throws Exception {
        ClienteResponse before = sampleCliente(1L);
        ClienteResponse after = ClienteResponse.builder()
                .id(1L).nombres("Carlos").apellidos("Lopez").dni("87654321")
                .email("c@t.com").telefono("888").direccion("Jr.")
                .tipoRelacion("SEPARACION").clienteDesde(LocalDate.now().plusDays(1))
                .loteId(2L).loteNumero(2).manzana("B")
                .parcelaNombre("P2").etapaNumero(2).projectId(2L).projectNombre("Otro").build();

        when(clienteService.getClienteById(1L)).thenReturn(before);
        when(clienteService.updateCliente(eq(1L), any())).thenReturn(after);

        String body = "{\"nombres\":\"Carlos\",\"apellidos\":\"Lopez\",\"dni\":\"87654321\"," +
                "\"email\":\"c@t.com\",\"telefono\":\"888\",\"direccion\":\"Jr.\"," +
                "\"loteId\":2,\"tipoRelacion\":\"SEPARACION\",\"clienteDesde\":\"2026-01-02\"}";

        mockMvc.perform(put("/api/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void updateCliente_notFound_returns400() throws Exception {
        when(clienteService.getClienteById(99L))
                .thenThrow(new RuntimeException("Cliente no encontrado"));

        String body = "{\"nombres\":\"X\",\"apellidos\":\"Y\",\"dni\":\"12345678\"," +
                "\"telefono\":\"999\",\"direccion\":\"Av.\"," +
                "\"loteId\":1,\"tipoRelacion\":\"ADQUISICION\",\"clienteDesde\":\"2026-01-01\"}";

        mockMvc.perform(put("/api/clientes/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ─── PUT /api/clientes/{id} — branches de construirDescripcionCambios ──────

    @Test
    void updateCliente_todosLosCamposCambian_returns200() throws Exception {
        ClienteResponse before = sampleCliente(1L);
        // Respuesta con TODOS los campos distintos → cubre todos los if de construirDescripcionCambios
        ClienteResponse after = ClienteResponse.builder()
                .id(1L).nombres("Carlos").apellidos("Lopez").dni("87654321")
                .email("c@t.com").telefono("888").direccion("Jr. Nuevo")
                .tipoRelacion("SEPARACION").clienteDesde(LocalDate.now().plusDays(5))
                .loteId(2L).loteNumero(2).manzana("B")
                .parcelaNombre("P2").etapaNumero(2).projectId(2L).projectNombre("Otro").build();

        when(clienteService.getClienteById(1L)).thenReturn(before);
        when(clienteService.updateCliente(eq(1L), any())).thenReturn(after);

        String body = "{\"nombres\":\"Carlos\",\"apellidos\":\"Lopez\",\"dni\":\"87654321\"," +
                "\"email\":\"c@t.com\",\"telefono\":\"888\",\"direccion\":\"Jr. Nuevo\"," +
                "\"loteId\":2,\"tipoRelacion\":\"SEPARACION\",\"clienteDesde\":\"2026-01-06\"}";

        mockMvc.perform(put("/api/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // El método registrarAccion debe haberse llamado con la descripción de cambios
        verify(registroAuditoriaService).registrarAccion(any(), eq("UPDATE"), contains("Carlos"));
    }

    // ─── POST /api/clientes/adquisiciones — branches de registrarIngresoAdquisicion ──

    @Test
    void createClientesPorAdquisicion_conMonto_registraIngreso() throws Exception {
        ClienteResponse created = sampleCliente(3L);
        when(clienteService.createClientesPorAdquisicion(any())).thenReturn(List.of(created));

        // Con montoOperacion > 0 → dispara registrarIngresoAdquisicion → INGRESO
        String body = "{\"loteId\":1,\"tipoOperacion\":\"CONTADO\"," +
                "\"fechaOperacion\":\"2026-01-01\",\"precioVenta\":50000," +
                "\"montoOperacion\":50000,\"propietarios\":[{" +
                "\"nombres\":\"Juan\",\"apellidos\":\"Perez\",\"dni\":\"12345678\"," +
                "\"telefono\":\"999\",\"direccion\":\"Av.\"}]}";

        mockMvc.perform(post("/api/clientes/adquisiciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void createClientesPorAdquisicion_listaVacia_returns201() throws Exception {
        when(clienteService.createClientesPorAdquisicion(any())).thenReturn(List.of());

        String body = "{\"loteId\":1,\"tipoOperacion\":\"SEPARACION\"," +
                "\"fechaOperacion\":\"2026-01-01\",\"precioVenta\":50000," +
                "\"montoOperacion\":5000,\"propietarios\":[{" +
                "\"nombres\":\"Juan\",\"apellidos\":\"Perez\",\"dni\":\"12345678\"," +
                "\"telefono\":\"999\",\"direccion\":\"Av.\"}]}";

        // Lista vacía → returna 201 sin registrar ingreso
        mockMvc.perform(post("/api/clientes/adquisiciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void createClientesPorAdquisicion_tipoSeparacion_normalizaTipo() throws Exception {
        ClienteResponse created = ClienteResponse.builder()
                .id(4L).nombres("Ana").apellidos("Torres").dni("87654321")
                .email("a@t.com").telefono("888").direccion("Jr.")
                .tipoRelacion("SEPARACION").clienteDesde(LocalDate.now())
                .loteId(1L).loteNumero(1).manzana("A")
                .parcelaNombre("P1").etapaNumero(1).projectId(1L).projectNombre("Proj").build();

        when(clienteService.createClientesPorAdquisicion(any())).thenReturn(List.of(created));

        String body = "{\"loteId\":1,\"tipoOperacion\":\"SEPARACION\"," +
                "\"fechaOperacion\":\"2026-01-01\",\"precioVenta\":50000," +
                "\"montoOperacion\":2000,\"propietarios\":[{" +
                "\"nombres\":\"Ana\",\"apellidos\":\"Torres\",\"dni\":\"87654321\"," +
                "\"telefono\":\"888\",\"direccion\":\"Jr.\"}]}";

        mockMvc.perform(post("/api/clientes/adquisiciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // normalizeTipoOperacion("SEPARACION") → "separación"
        verify(registroAuditoriaService).registrarAccion(any(), eq("ADQUISICION_LOTE"),
                contains("separación"));
    }

    // ─── DELETE /api/clientes/{id} ───────────────────────────────────────

    @Test
    void deleteCliente_existing_returns204() throws Exception {
        when(clienteService.getClienteById(1L)).thenReturn(sampleCliente(1L));
        doNothing().when(clienteService).deleteCliente(1L);

        mockMvc.perform(delete("/api/clientes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCliente_notFound_returns400() throws Exception {
        when(clienteService.getClienteById(99L))
                .thenThrow(new RuntimeException("Cliente no encontrado"));

        mockMvc.perform(delete("/api/clientes/99"))
                .andExpect(status().isBadRequest());
    }
}
