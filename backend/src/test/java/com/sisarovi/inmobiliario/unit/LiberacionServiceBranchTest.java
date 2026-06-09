package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.dto.LiberacionLoteResponse;
import com.sisarovi.inmobiliario.entity.*;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.repository.CronogramaContratoRepository;
import com.sisarovi.inmobiliario.service.AdminService;
import com.sisarovi.inmobiliario.service.LiberacionService;
import com.sisarovi.inmobiliario.service.RegistroAuditoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests de branches específicos de LiberacionService:
 * isMoroso, resolveEstadoVisual, titulares múltiples, lote sin parcela/manzana
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LiberacionServiceBranchTest {

    @Mock ClienteRepository clienteRepository;
    @Mock CronogramaContratoRepository cronogramaContratoRepository;
    @Mock AdminService adminService;
    @Mock RegistroAuditoriaService registroAuditoriaService;

    @InjectMocks LiberacionService liberacionService;

    private Lote lote;
    private Cliente clientePrincipal;
    private Cliente clienteSecundario;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().name("ROLE_ADMIN").build();
        setField(role, "id", 1L);
        User user = User.builder().dni("admin").nombres("Admin").primerApellido("T")
                .segundoApellido("").password("pw").email("a@b.com").role(role)
                .estado(UserStatus.ACTIVO).enabled(true).build();
        setField(user, "id", 1L);

        Project project = new Project();
        setField(project, "id", 1L);
        project.setNombre("Proyecto");
        project.setCantidadEtapas(1);
        project.setCreatedBy(user);
        project.setEtapas(new ArrayList<>());

        Etapa etapa = new Etapa();
        setField(etapa, "id", 1L);
        etapa.setNumeroEtapa(1);
        etapa.setProject(project);
        etapa.setParcelas(new ArrayList<>());

        Parcela parcela = new Parcela();
        setField(parcela, "id", 1L);
        parcela.setNombre("Parcela A");
        parcela.setNumManzanas(1);
        parcela.setNumLotes(0);
        parcela.setPropietario("Owner");
        parcela.setLotesDisponibles(0);
        parcela.setEtapa(etapa);
        parcela.setLotes(new ArrayList<>());

        Manzana manzana = new Manzana();
        setField(manzana, "id", 1L);
        manzana.setNombre("Manzana A");
        manzana.setParcela(parcela);

        lote = new Lote();
        setField(lote, "id", 1L);
        lote.setNumero(1);
        lote.setNumeroPartida("12345678");
        lote.setPrecioLote(new BigDecimal("50000"));
        lote.setParcela(parcela);
        lote.setManzana(manzana);

        clientePrincipal = Cliente.builder()
                .nombres("Juan").apellidos("Perez").dni("12345678")
                .email("j@t.com").telefono("999").direccion("Av.")
                .tipoRelacion("ADQUISICION").clienteDesde(LocalDate.now()).lote(lote).build();
        setField(clientePrincipal, "id", 1L);

        clienteSecundario = Cliente.builder()
                .nombres("Ana").apellidos("Torres").dni("87654321")
                .email("a@t.com").telefono("888").direccion("Jr.")
                .tipoRelacion("ADQUISICION").clienteDesde(LocalDate.now()).lote(lote).build();
        setField(clienteSecundario, "id", 2L);
    }

    // ─── listar: resolveEstadoVisual branches ─────────────────────────

    @Test
    void listar_sinContrato_estadoVisualSinCronograma() {
        when(clienteRepository.findByProjectIdOrdered(1L)).thenReturn(List.of(clientePrincipal));
        when(cronogramaContratoRepository.findDetailedByLoteIds(List.of(1L))).thenReturn(List.of());

        List<LiberacionLoteResponse> result = liberacionService.listarLotesAdquiridosPorProyecto(1L);

        assertEquals("Sin cronograma", result.get(0).getEstadoVisual());
        assertFalse(result.get(0).isMoroso());
    }

    @Test
    void listar_contratoAlDia_estadoVisualAlDia() {
        CronogramaContrato contrato = buildContrato("AL_DIA", new BigDecimal("50000"), new ArrayList<>());
        contrato.setCliente(clientePrincipal);

        when(clienteRepository.findByProjectIdOrdered(1L)).thenReturn(List.of(clientePrincipal));
        when(cronogramaContratoRepository.findDetailedByLoteIds(List.of(1L))).thenReturn(List.of(contrato));

        List<LiberacionLoteResponse> result = liberacionService.listarLotesAdquiridosPorProyecto(1L);

        assertEquals("Al día", result.get(0).getEstadoVisual());
        assertFalse(result.get(0).isMoroso());
    }

    @Test
    void listar_contratoDeudor_estadoVisualPagoCuotas_moroso() {
        CronogramaContrato contrato = buildContrato("DEUDOR", new BigDecimal("10000"), new ArrayList<>());
        contrato.setCliente(clientePrincipal);

        when(clienteRepository.findByProjectIdOrdered(1L)).thenReturn(List.of(clientePrincipal));
        when(cronogramaContratoRepository.findDetailedByLoteIds(List.of(1L))).thenReturn(List.of(contrato));

        List<LiberacionLoteResponse> result = liberacionService.listarLotesAdquiridosPorProyecto(1L);

        assertEquals("Pago de cuotas en curso", result.get(0).getEstadoVisual());
        assertTrue(result.get(0).isMoroso());
    }

    @Test
    void listar_separacionEnCurso_estadoVisualSeparacion() {
        CronogramaContrato contrato = buildContrato("SEPARACION_EN_CURSO", BigDecimal.ZERO, new ArrayList<>());
        contrato.setCliente(clientePrincipal);
        contrato.setMontoSeparacionAcumulado(new BigDecimal("500"));
        contrato.setMontoSeparacionObjetivo(new BigDecimal("2000"));

        when(clienteRepository.findByProjectIdOrdered(1L)).thenReturn(List.of(clientePrincipal));
        when(cronogramaContratoRepository.findDetailedByLoteIds(List.of(1L))).thenReturn(List.of(contrato));

        List<LiberacionLoteResponse> result = liberacionService.listarLotesAdquiridosPorProyecto(1L);

        assertEquals("Separación en curso", result.get(0).getEstadoVisual());
    }

    @Test
    void listar_cuotasPendientesSaldo_estadoVisualPagoCuotas() {
        CronogramaCuota cuota = new CronogramaCuota();
        cuota.setNumeroCuota(1);
        cuota.setMontoCuota(new BigDecimal("1500"));
        cuota.setMontoPagado(BigDecimal.ZERO);
        cuota.setEstadoPago("PENDIENTE");
        cuota.setFechaVencimiento(LocalDate.now().plusMonths(1));
        cuota.setPagos(new ArrayList<>());

        CronogramaContrato contrato = buildContrato("AL_DIA", new BigDecimal("10000"), List.of(cuota));
        contrato.setCliente(clientePrincipal);
        cuota.setContrato(contrato);

        when(clienteRepository.findByProjectIdOrdered(1L)).thenReturn(List.of(clientePrincipal));
        when(cronogramaContratoRepository.findDetailedByLoteIds(List.of(1L))).thenReturn(List.of(contrato));

        List<LiberacionLoteResponse> result = liberacionService.listarLotesAdquiridosPorProyecto(1L);

        assertEquals("Pago de cuotas en curso", result.get(0).getEstadoVisual());
    }

    @Test
    void listar_cuotaVencidaSinPagar_moroso() {
        CronogramaCuota cuota = new CronogramaCuota();
        cuota.setNumeroCuota(1);
        cuota.setMontoCuota(new BigDecimal("1500"));
        cuota.setMontoPagado(BigDecimal.ZERO);
        cuota.setEstadoPago("PENDIENTE");
        cuota.setFechaVencimiento(LocalDate.now().minusDays(10)); // vencida
        cuota.setPagos(new ArrayList<>());

        CronogramaContrato contrato = buildContrato("AL_DIA", new BigDecimal("10000"), List.of(cuota));
        contrato.setCliente(clientePrincipal);
        cuota.setContrato(contrato);

        when(clienteRepository.findByProjectIdOrdered(1L)).thenReturn(List.of(clientePrincipal));
        when(cronogramaContratoRepository.findDetailedByLoteIds(List.of(1L))).thenReturn(List.of(contrato));

        List<LiberacionLoteResponse> result = liberacionService.listarLotesAdquiridosPorProyecto(1L);

        assertTrue(result.get(0).isMoroso());
    }

    // ─── listar: múltiples titulares ─────────────────────────────────

    @Test
    void listar_dosTitulares_losMuestraJuntos() {
        when(clienteRepository.findByProjectIdOrdered(1L))
                .thenReturn(List.of(clientePrincipal, clienteSecundario));
        when(cronogramaContratoRepository.findDetailedByLoteIds(List.of(1L))).thenReturn(List.of());

        List<LiberacionLoteResponse> result = liberacionService.listarLotesAdquiridosPorProyecto(1L);

        assertEquals(1, result.size()); // mismo lote → agrupados
        assertEquals(2, result.get(0).getCantidadTitulares());
        assertTrue(result.get(0).getTitulares().contains("Juan"));
        assertTrue(result.get(0).getTitulares().contains("Ana"));
    }

    // ─── liberarLote: titular con nombres vacíos ──────────────────────

    @Test
    void liberarLote_nombreVacio_usaSinNombre() {
        Cliente sinNombre = Cliente.builder()
                .nombres("").apellidos("").dni("11111111")
                .email(null).telefono("999").direccion("Av.")
                .tipoRelacion("ADQUISICION").clienteDesde(LocalDate.now()).lote(lote).build();
        setField(sinNombre, "id", 3L);

        when(clienteRepository.findAllByLoteId(1L)).thenReturn(List.of(sinNombre));
        when(cronogramaContratoRepository.findByClienteIdIn(List.of(3L))).thenReturn(List.of());
        doNothing().when(cronogramaContratoRepository).deleteByClienteIdIn(any());
        doNothing().when(clienteRepository).deleteAll(any());
        doNothing().when(registroAuditoriaService).registrarAccion(
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any());

        // No debe lanzar excepción
        assertDoesNotThrow(() ->
                liberacionService.liberarLote(1L, "motivo", null, "admin"));
    }

    // ─── helpers ─────────────────────────────────────────────────────

    private CronogramaContrato buildContrato(String estado, BigDecimal pagado,
                                              List<CronogramaCuota> cuotas) {
        CronogramaContrato c = CronogramaContrato.builder()
                .tipoOperacion("CREDITO")
                .estado(estado)
                .fechaOperacion(LocalDate.now())
                .precioVenta(new BigDecimal("50000"))
                .montoPagadoTotal(pagado)
                .montoSeparacionObjetivo(new BigDecimal("2000"))
                .montoSeparacionAcumulado(new BigDecimal("2000"))
                .saldoFinanciarInicial(new BigDecimal("45000"))
                .plazoMeses(12)
                .interesPorcentaje(BigDecimal.ZERO)
                .montoCuotaReferencial(new BigDecimal("3750"))
                .pagosSeparacion(new ArrayList<>())
                .build();
        c.setCuotas(new ArrayList<>(cuotas));
        return c;
    }

    private void setField(Object obj, String name, Object value) {
        try {
            var f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            try {
                var f = obj.getClass().getSuperclass().getDeclaredField(name);
                f.setAccessible(true);
                f.set(obj, value);
            } catch (Exception ignored) {}
        }
    }
}
