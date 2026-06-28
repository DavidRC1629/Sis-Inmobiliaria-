package com.sisarovi.inmobiliario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.CronogramaContratoResponse;
import com.sisarovi.inmobiliario.dto.LiberacionLoteResponse;
import com.sisarovi.inmobiliario.dto.LoteResponse;
import com.sisarovi.inmobiliario.exception.GlobalExceptionHandler;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.repository.LoteRepository;
import com.sisarovi.inmobiliario.repository.ManzanaRepository;
import com.sisarovi.inmobiliario.repository.ParcelaRepository;
import com.sisarovi.inmobiliario.repository.RegistroAuditoriaRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import com.sisarovi.inmobiliario.service.AppSettingService;
import com.sisarovi.inmobiliario.service.CronogramaService;
import com.sisarovi.inmobiliario.service.DniApiService;
import com.sisarovi.inmobiliario.service.LiberacionService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests para: AppSettingController, ManzanaController, LotePorManzanaController,
 *             LiberacionController, ReniecController, CronogramaController
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MiscControllerTest {

    final ObjectMapper objectMapper = new ObjectMapper();

    // ──────────────────────────────────────────────────────────────────────
    // Shared auth resolver
    // ──────────────────────────────────────────────────────────────────────

    private static final UsernamePasswordAuthenticationToken ADMIN_AUTH =
            new UsernamePasswordAuthenticationToken("admin", null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    private static final HandlerMethodArgumentResolver AUTH_RESOLVER = new HandlerMethodArgumentResolver() {
        @Override public boolean supportsParameter(MethodParameter p) {
            return Authentication.class.isAssignableFrom(p.getParameterType());
        }
        @Override public Object resolveArgument(MethodParameter p, ModelAndViewContainer m,
                                                NativeWebRequest r, WebDataBinderFactory f) { return ADMIN_AUTH; }
    };

    // ════════════════════════════════════════════════════════════════════════
    // AppSettingController
    // ════════════════════════════════════════════════════════════════════════

    @Mock AppSettingService appSettingService;
    @InjectMocks AppSettingController appSettingController;

    private MockMvc appSettingMvc() {
        return MockMvcBuilders.standaloneSetup(appSettingController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void getLogoArovi_returns200() throws Exception {
        when(appSettingService.getLogoAroviUrl()).thenReturn("http://logo.png");

        appSettingMvc().perform(get("/api/settings/logo-arovi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoAroviUrl").value("http://logo.png"));
    }

    @Test
    void updateLogoArovi_valid_returns200() throws Exception {
        when(appSettingService.saveLogoAroviUrl("http://new.png")).thenReturn("http://new.png");

        appSettingMvc().perform(put("/api/settings/logo-arovi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logoAroviUrl\":\"http://new.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoAroviUrl").value("http://new.png"));
    }

    @Test
    void updateLogoArovi_nullBody_usesEmpty() throws Exception {
        when(appSettingService.saveLogoAroviUrl("")).thenReturn("");

        appSettingMvc().perform(put("/api/settings/logo-arovi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    // ════════════════════════════════════════════════════════════════════════
    // ManzanaController
    // ════════════════════════════════════════════════════════════════════════

    @Mock ManzanaRepository manzanaRepository;
    @Mock ParcelaRepository parcelaRepository;
    @InjectMocks ManzanaController manzanaController;

    private MockMvc manzanaMvc() {
        return MockMvcBuilders.standaloneSetup(manzanaController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void getManzanasByParcela_existing_returns200() throws Exception {
        com.sisarovi.inmobiliario.entity.Manzana m = new com.sisarovi.inmobiliario.entity.Manzana();
        m.setNombre("Manzana A");
        setField(m, "id", 1L);

        when(manzanaRepository.findByParcelaIdOrderByNombreAsc(1L)).thenReturn(List.of(m));

        manzanaMvc().perform(get("/api/parcelas/1/manzanas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Manzana A"));
    }

    @Test
    void getManzanasByParcela_empty_createsAndReturns() throws Exception {
        com.sisarovi.inmobiliario.entity.Etapa etapa = new com.sisarovi.inmobiliario.entity.Etapa();
        etapa.setNumeroEtapa(1);
        etapa.setParcelas(new java.util.ArrayList<>());

        com.sisarovi.inmobiliario.entity.Parcela parcela = new com.sisarovi.inmobiliario.entity.Parcela();
        setField(parcela, "id", 1L);
        parcela.setNombre("Parcela A");
        parcela.setNumManzanas(2);
        parcela.setNumLotes(0);
        parcela.setPropietario("Owner");
        parcela.setLotesDisponibles(0);
        parcela.setEtapa(etapa);
        parcela.setLotes(new java.util.ArrayList<>());

        com.sisarovi.inmobiliario.entity.Manzana saved = new com.sisarovi.inmobiliario.entity.Manzana();
        saved.setNombre("Manzana A");
        setField(saved, "id", 1L);

        // Primera llamada vacía, segunda con datos
        when(manzanaRepository.findByParcelaIdOrderByNombreAsc(1L))
                .thenReturn(List.of())
                .thenReturn(List.of(saved));
        when(parcelaRepository.findById(1L)).thenReturn(java.util.Optional.of(parcela));
        when(manzanaRepository.save(any())).thenReturn(saved);

        manzanaMvc().perform(get("/api/parcelas/1/manzanas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Manzana A"));
    }

    @Test
    void getManzanasByParcela_parcelaNotFound_returns400() throws Exception {
        when(manzanaRepository.findByParcelaIdOrderByNombreAsc(99L)).thenReturn(List.of());
        when(parcelaRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        manzanaMvc().perform(get("/api/parcelas/99/manzanas"))
                .andExpect(status().isBadRequest());
    }

    // ════════════════════════════════════════════════════════════════════════
    // LotePorManzanaController
    // ════════════════════════════════════════════════════════════════════════

    @Mock LoteRepository loteRepository;
    @Mock ClienteRepository clienteRepository;
    @InjectMocks LotePorManzanaController lotePorManzanaController;

    private MockMvc loteManzanaMvc() {
        return MockMvcBuilders.standaloneSetup(lotePorManzanaController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void getLotesByManzana_returns200() throws Exception {
        com.sisarovi.inmobiliario.entity.Lote lote = buildLote();
        when(loteRepository.findByManzanaIdOrderByNumeroAsc(1L)).thenReturn(List.of(lote));
        when(clienteRepository.existsByLoteId(anyLong())).thenReturn(false);

        loteManzanaMvc().perform(get("/api/manzanas/1/lotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value(1));
    }

    @Test
    void getLotesByManzana_empty_returns200() throws Exception {
        when(loteRepository.findByManzanaIdOrderByNumeroAsc(99L)).thenReturn(List.of());

        loteManzanaMvc().perform(get("/api/manzanas/99/lotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getLotesByManzana_loteAdquirido_returnsAdquiridoTrue() throws Exception {
        com.sisarovi.inmobiliario.entity.Lote lote = buildLote();
        when(loteRepository.findByManzanaIdOrderByNumeroAsc(1L)).thenReturn(List.of(lote));
        when(clienteRepository.existsByLoteId(anyLong())).thenReturn(true);

        loteManzanaMvc().perform(get("/api/manzanas/1/lotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adquirido").value(true));
    }

    // ════════════════════════════════════════════════════════════════════════
    // LiberacionController
    // ════════════════════════════════════════════════════════════════════════

    @Mock LiberacionService liberacionService;
    @InjectMocks LiberacionController liberacionController;

    private MockMvc liberacionMvc() {
        return MockMvcBuilders.standaloneSetup(liberacionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(AUTH_RESOLVER).build();
    }

    @Test
    void listarLotesAdquiridos_returns200() throws Exception {
        when(liberacionService.listarLotesAdquiridosPorProyecto(1L)).thenReturn(List.of(
                LiberacionLoteResponse.builder().loteId(1L).loteNumero(1)
                        .manzana("A").parcelaNombre("P1").etapaNumero(1)
                        .projectId(1L).projectNombre("Proj").titulares("Juan")
                        .titularesDni("12345678").cantidadTitulares(1)
                        .tipoOperacion("CONTADO").estadoCronograma("AL_DIA")
                        .estadoVisual("Al día").moroso(false)
                        .montoPagadoTotal(new BigDecimal("50000"))
                        .requierePasswordAdmin(false).build()
        ));

        liberacionMvc().perform(get("/api/liberaciones/lotes").param("projectId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].loteNumero").value(1));
    }

    @Test
    void listarLotesAdquiridos_empty_returns200() throws Exception {
        when(liberacionService.listarLotesAdquiridosPorProyecto(99L)).thenReturn(List.of());

        liberacionMvc().perform(get("/api/liberaciones/lotes").param("projectId", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void liberarLote_valid_returns204() throws Exception {
        doNothing().when(liberacionService).liberarLote(eq(1L), any(), any(), any());

        String body = "{\"descripcion\":\"Motivo\",\"adminPassword\":\"admin123\"}";

        liberacionMvc().perform(post("/api/liberaciones/lotes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void liberarLote_noTitulares_returns400() throws Exception {
        doThrow(new RuntimeException("El lote ya no tiene titulares asociados."))
                .when(liberacionService).liberarLote(eq(99L), any(), any(), any());

        String body = "{\"descripcion\":\"Motivo\",\"adminPassword\":\"admin123\"}";

        liberacionMvc().perform(post("/api/liberaciones/lotes/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ════════════════════════════════════════════════════════════════════════
    // ReniecController
    // ════════════════════════════════════════════════════════════════════════

    @Mock DniApiService dniApiService;
    @InjectMocks ReniecController reniecController;

    private MockMvc reniecMvc() {
        return MockMvcBuilders.standaloneSetup(reniecController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void getReniecData_found_returns200() throws Exception {
        org.json.JSONObject json = new org.json.JSONObject();
        json.put("name", "JUAN");
        json.put("surname", "PEREZ LOPEZ");
        when(dniApiService.consultarPorDniJson("12345678")).thenReturn(json);

        reniecMvc().perform(get("/api/reniec/dni/12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("JUAN"));
    }

    @Test
    void getReniecData_notFound_returns404() throws Exception {
        when(dniApiService.consultarPorDniJson("99999999")).thenReturn(null);

        reniecMvc().perform(get("/api/reniec/dni/99999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReniecData_serviceUnavailable_returns503() throws Exception {
        when(dniApiService.consultarPorDniJson("12345678"))
                .thenThrow(new com.sisarovi.inmobiliario.exception.ReniecServiceUnavailableException(
                        "RENIEC no disponible", "timeout"));

        reniecMvc().perform(get("/api/reniec/dni/12345678"))
                .andExpect(status().isServiceUnavailable());
    }

    // ════════════════════════════════════════════════════════════════════════
    // CronogramaController
    // ════════════════════════════════════════════════════════════════════════

    @Mock CronogramaService cronogramaService;
    @InjectMocks CronogramaController cronogramaController;

    private MockMvc cronogramaMvc() {
        return MockMvcBuilders.standaloneSetup(cronogramaController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private CronogramaContratoResponse sampleCronograma() {
        return CronogramaContratoResponse.builder()
                .id(1L).clienteId(1L).clienteNombre("Juan").clienteDni("12345678")
                .asesor("Asesor").projectId(1L).projectNombre("Proj")
                .etapaNumero(1).parcelaNombre("P1").manzana("A")
                .loteId(1L).loteNumero(1).tipoOperacion("CREDITO")
                .estado("AL_DIA").precioVenta(new BigDecimal("50000"))
                .montoPagadoTotal(new BigDecimal("10000"))
                .montoSeparacionObjetivo(new BigDecimal("2000"))
                .montoSeparacionAcumulado(new BigDecimal("2000"))
                .saldoFinanciarInicial(new BigDecimal("40000"))
                .saldoPendienteActual(new BigDecimal("30000"))
                .plazoMeses(24).interesPorcentaje(new BigDecimal("10"))
                .montoCuotaReferencial(new BigDecimal("1500"))
                .pagosSeparacion(List.of()).cuotas(List.of()).build();
    }

    @Test
    void listar_sinFiltros_returns200() throws Exception {
        when(cronogramaService.listar(any())).thenReturn(List.of(sampleCronograma()));

        cronogramaMvc().perform(get("/api/cronogramas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void listar_conFiltros_returns200() throws Exception {
        when(cronogramaService.listar(any())).thenReturn(List.of());

        cronogramaMvc().perform(get("/api/cronogramas")
                        .param("projectId", "1").param("estado", "AL_DIA"))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerPorId_existing_returns200() throws Exception {
        when(cronogramaService.obtenerPorId(1L)).thenReturn(sampleCronograma());

        cronogramaMvc().perform(get("/api/cronogramas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("AL_DIA"));
    }

    @Test
    void obtenerPorId_notFound_returns400() throws Exception {
        when(cronogramaService.obtenerPorId(99L))
                .thenThrow(new RuntimeException("Cronograma no encontrado"));

        cronogramaMvc().perform(get("/api/cronogramas/99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarAsesor_valid_returns200() throws Exception {
        when(cronogramaService.actualizarAsesor(eq(1L), eq("Nuevo Asesor")))
                .thenReturn(sampleCronograma());

        cronogramaMvc().perform(patch("/api/cronogramas/1/asesor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asesor\":\"Nuevo Asesor\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void actualizarAsesor_asesorVacio_usesDefault() throws Exception {
        when(cronogramaService.actualizarAsesor(eq(1L), eq("")))
                .thenReturn(sampleCronograma());

        cronogramaMvc().perform(patch("/api/cronogramas/1/asesor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void registrarPagoSeparacion_valid_returns200() throws Exception {
        when(cronogramaService.registrarPagoSeparacion(eq(1L), any()))
                .thenReturn(sampleCronograma());

        String body = "{\"monto\":500,\"fechaPago\":\"2026-06-01\",\"observacion\":\"Pago 1\"}";

        cronogramaMvc().perform(post("/api/cronogramas/1/pagos/separacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void registrarPagoSeparacion_notFound_returns400() throws Exception {
        when(cronogramaService.registrarPagoSeparacion(eq(99L), any()))
                .thenThrow(new RuntimeException("Cronograma no encontrado"));

        String body = "{\"monto\":500,\"fechaPago\":\"2026-06-01\"}";

        cronogramaMvc().perform(post("/api/cronogramas/99/pagos/separacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarPagoCuota_valid_returns200() throws Exception {
        when(cronogramaService.registrarPagoCuota(eq(1L), any()))
                .thenReturn(sampleCronograma());

        String body = "{\"monto\":1500,\"fechaPago\":\"2026-06-01\"}";

        cronogramaMvc().perform(post("/api/cronogramas/cuotas/1/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void aplicarDescuento_valid_returns200() throws Exception {
        when(cronogramaService.aplicarDescuento(any())).thenReturn(sampleCronograma());

        String body = "{\"clienteId\":1,\"montoDescuento\":500,\"observacion\":\"desc\"}";

        cronogramaMvc().perform(post("/api/cronogramas/aplicar-descuento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    // ════════════════════════════════════════════════════════════════════════
    // RegistroAuditoriaController
    // ════════════════════════════════════════════════════════════════════════

    @Mock RegistroAuditoriaRepository registroAuditoriaRepository;
    @Mock UserRepository userRepository;
    @InjectMocks RegistroAuditoriaController registroAuditoriaController;

    private MockMvc auditoriaMvc() {
        return MockMvcBuilders.standaloneSetup(registroAuditoriaController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private com.sisarovi.inmobiliario.entity.RegistroAuditoria buildRegistro(Long id) {
        com.sisarovi.inmobiliario.entity.RegistroAuditoria r =
                com.sisarovi.inmobiliario.entity.RegistroAuditoria.builder()
                        .usuario("admin").accion("CREATE").descripcion("desc")
                        .fechaHora(java.time.LocalDateTime.now()).build();
        setField(r, "id", id);
        return r;
    }

    @Test
    void getAllRegistros_sinFiltros_returns200() throws Exception {
        when(registroAuditoriaRepository.findAllByOrderByFechaHoraDesc())
                .thenReturn(List.of(buildRegistro(1L)));
        when(userRepository.findByDni(any())).thenReturn(java.util.Optional.empty());

        auditoriaMvc().perform(get("/api/registro-auditoria"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllRegistros_conFechaDesde_returns200() throws Exception {
        when(registroAuditoriaRepository.findByFechaHoraGreaterThanEqualOrderByFechaHoraDesc(any()))
                .thenReturn(List.of(buildRegistro(1L)));
        when(userRepository.findByDni(any())).thenReturn(java.util.Optional.empty());

        auditoriaMvc().perform(get("/api/registro-auditoria")
                        .param("fechaDesde", "2026-01-01"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllRegistros_conFechaHasta_returns200() throws Exception {
        when(registroAuditoriaRepository.findByFechaHoraLessThanEqualOrderByFechaHoraDesc(any()))
                .thenReturn(List.of(buildRegistro(1L)));
        when(userRepository.findByDni(any())).thenReturn(java.util.Optional.empty());

        auditoriaMvc().perform(get("/api/registro-auditoria")
                        .param("fechaHasta", "2026-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllRegistros_conRango_returns200() throws Exception {
        when(registroAuditoriaRepository.findByFechaHoraBetween(any(), any()))
                .thenReturn(List.of(buildRegistro(1L)));
        when(userRepository.findByDni(any())).thenReturn(java.util.Optional.empty());

        auditoriaMvc().perform(get("/api/registro-auditoria")
                        .param("fechaDesde", "2026-01-01")
                        .param("fechaHasta", "2026-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllRegistros_conTipo_filtra() throws Exception {
        com.sisarovi.inmobiliario.entity.RegistroAuditoria r = buildRegistro(1L);
        r.setAccion("CREATE");
        when(registroAuditoriaRepository.findAllByOrderByFechaHoraDesc())
                .thenReturn(List.of(r));
        when(userRepository.findByDni(any())).thenReturn(java.util.Optional.empty());

        auditoriaMvc().perform(get("/api/registro-auditoria").param("tipo", "CREATE"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllRegistros_loginSeOmite() throws Exception {
        com.sisarovi.inmobiliario.entity.RegistroAuditoria login = buildRegistro(2L);
        login.setAccion("LOGIN");
        when(registroAuditoriaRepository.findAllByOrderByFechaHoraDesc())
                .thenReturn(List.of(login));
        when(userRepository.findByDni(any())).thenReturn(java.util.Optional.empty());

        auditoriaMvc().perform(get("/api/registro-auditoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void actualizarItem_valid_returns200() throws Exception {
        com.sisarovi.inmobiliario.entity.RegistroAuditoria r = buildRegistro(1L);
        when(registroAuditoriaRepository.findById(1L)).thenReturn(java.util.Optional.of(r));
        when(registroAuditoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByDni(any())).thenReturn(java.util.Optional.empty());

        auditoriaMvc().perform(put("/api/registro-auditoria/1/item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":\"Pago de Cuota\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void actualizarItem_notFound_returns4xx() throws Exception {
        when(registroAuditoriaRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        // ResponseStatusException es capturada por GlobalExceptionHandler como RuntimeException → 400
        auditoriaMvc().perform(put("/api/registro-auditoria/99/item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":\"X\"}"))
                .andExpect(status().is4xxClientError());
    }

    // ─── helpers ─────────────────────────────────────────────────────────

    private com.sisarovi.inmobiliario.entity.Lote buildLote() {
        com.sisarovi.inmobiliario.entity.Role role = com.sisarovi.inmobiliario.entity.Role.builder().name("ROLE_ADMIN").build();
        setField(role, "id", 1L);
        com.sisarovi.inmobiliario.entity.User user = com.sisarovi.inmobiliario.entity.User.builder()
                .dni("admin").nombres("A").primerApellido("B").segundoApellido("").password("pw")
                .email("a@b.com").role(role).estado(com.sisarovi.inmobiliario.entity.UserStatus.ACTIVO).enabled(true).build();
        setField(user, "id", 1L);

        com.sisarovi.inmobiliario.entity.Project project = new com.sisarovi.inmobiliario.entity.Project();
        setField(project, "id", 1L);
        project.setNombre("Proj");
        project.setCantidadEtapas(1);
        project.setCreatedBy(user);
        project.setEtapas(new java.util.ArrayList<>());

        com.sisarovi.inmobiliario.entity.Etapa etapa = new com.sisarovi.inmobiliario.entity.Etapa();
        setField(etapa, "id", 1L);
        etapa.setNumeroEtapa(1);
        etapa.setProject(project);
        etapa.setParcelas(new java.util.ArrayList<>());

        com.sisarovi.inmobiliario.entity.Parcela parcela = new com.sisarovi.inmobiliario.entity.Parcela();
        setField(parcela, "id", 1L);
        parcela.setNombre("P1");
        parcela.setNumManzanas(1);
        parcela.setNumLotes(0);
        parcela.setPropietario("O");
        parcela.setLotesDisponibles(0);
        parcela.setEtapa(etapa);
        parcela.setLotes(new java.util.ArrayList<>());

        com.sisarovi.inmobiliario.entity.Manzana manzana = new com.sisarovi.inmobiliario.entity.Manzana();
        setField(manzana, "id", 1L);
        manzana.setNombre("Manzana A");
        manzana.setParcela(parcela);

        com.sisarovi.inmobiliario.entity.Lote lote = new com.sisarovi.inmobiliario.entity.Lote();
        setField(lote, "id", 1L);
        lote.setNumero(1);
        lote.setNumeroPartida("12345678");
        lote.setPrecioLote(new BigDecimal("50000"));
        lote.setParcela(parcela);
        lote.setManzana(manzana);
        return lote;
    }

    private void setField(Object obj, String name, Object value) {
        try {
            var f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception ignored) {}
    }
}
