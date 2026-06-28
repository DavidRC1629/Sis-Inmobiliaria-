package com.sisarovi.inmobiliario.unit;

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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LiberacionServiceUnitTest {

    @Mock ClienteRepository clienteRepository;
    @Mock CronogramaContratoRepository cronogramaContratoRepository;
    @Mock AdminService adminService;
    @Mock RegistroAuditoriaService registroAuditoriaService;

    @InjectMocks LiberacionService liberacionService;

    private Lote lote;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().name("ROLE_USER").build();
        setField(role, "id", 1L);
        User user = User.builder().dni("admin").nombres("A").primerApellido("B").segundoApellido("")
                .password("pw").email("a@b.com").role(role).estado(UserStatus.ACTIVO).enabled(true).build();
        setField(user, "id", 1L);

        Project project = new Project();
        setField(project, "id", 1L);
        project.setNombre("Proyecto Alpha");
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

        cliente = Cliente.builder()
                .nombres("Juan").apellidos("Perez").dni("12345678")
                .email("j@t.com").telefono("999").direccion("Av.")
                .tipoRelacion("ADQUISICION").clienteDesde(LocalDate.now()).lote(lote)
                .build();
        setField(cliente, "id", 1L);
    }

    // ─── listarLotesAdquiridosPorProyecto ────────────────────────────

    @Test
    void listar_sinClientes_returnsEmpty() {
        when(clienteRepository.findByProjectIdOrdered(1L)).thenReturn(List.of());

        var result = liberacionService.listarLotesAdquiridosPorProyecto(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void listar_clienteSinLote_returnsEmpty() {
        Cliente sinLote = Cliente.builder()
                .nombres("Ana").apellidos("Torres").dni("87654321")
                .email("a@t.com").telefono("888").direccion("Jr.")
                .tipoRelacion("ADQUISICION").clienteDesde(LocalDate.now()).lote(null)
                .build();
        setField(sinLote, "id", 2L);

        when(clienteRepository.findByProjectIdOrdered(1L)).thenReturn(List.of(sinLote));
        when(cronogramaContratoRepository.findDetailedByLoteIds(any())).thenReturn(List.of());

        var result = liberacionService.listarLotesAdquiridosPorProyecto(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void listar_conCliente_returnsResponse() {
        when(clienteRepository.findByProjectIdOrdered(1L)).thenReturn(List.of(cliente));
        when(cronogramaContratoRepository.findDetailedByLoteIds(List.of(1L))).thenReturn(List.of());

        var result = liberacionService.listarLotesAdquiridosPorProyecto(1L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getLoteNumero());
        assertEquals("Proyecto Alpha", result.get(0).getProjectNombre());
    }

    // ─── liberarLote ─────────────────────────────────────────────────

    @Test
    void liberarLote_sinTitulares_throws() {
        when(clienteRepository.findAllByLoteId(1L)).thenReturn(List.of());

        assertThrows(RuntimeException.class,
                () -> liberacionService.liberarLote(1L, "motivo", "admin123", "admin"));
    }

    @Test
    void liberarLote_totalPagadoCero_noValidaPassword() {
        when(clienteRepository.findAllByLoteId(1L)).thenReturn(List.of(cliente));
        when(cronogramaContratoRepository.findByClienteIdIn(List.of(1L))).thenReturn(List.of());
        doNothing().when(cronogramaContratoRepository).deleteByClienteIdIn(any());
        doNothing().when(clienteRepository).deleteAll(any());
        doNothing().when(registroAuditoriaService).registrarAccion(
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any());

        // totalPagado = 0 → no debe llamar validarPasswordAdmin
        liberacionService.liberarLote(1L, "motivo", null, "admin");

        verify(adminService, never()).validarPasswordAdmin(any(), any());
        verify(cronogramaContratoRepository).deleteByClienteIdIn(any());
        verify(clienteRepository).deleteAll(List.of(cliente));
    }

    @Test
    void liberarLote_conPagosRealizados_validaPassword() {
        CronogramaContrato contrato = CronogramaContrato.builder()
                .cliente(cliente)
                .tipoOperacion("CREDITO")
                .estado("AL_DIA")
                .fechaOperacion(LocalDate.now())
                .precioVenta(new BigDecimal("50000"))
                .montoPagadoTotal(new BigDecimal("5000"))
                .montoSeparacionObjetivo(new BigDecimal("2000"))
                .montoSeparacionAcumulado(new BigDecimal("2000"))
                .saldoFinanciarInicial(new BigDecimal("45000"))
                .plazoMeses(12)
                .interesPorcentaje(BigDecimal.ZERO)
                .montoCuotaReferencial(new BigDecimal("3750"))
                .cuotas(new ArrayList<>())
                .pagosSeparacion(new ArrayList<>())
                .build();

        when(clienteRepository.findAllByLoteId(1L)).thenReturn(List.of(cliente));
        when(cronogramaContratoRepository.findByClienteIdIn(List.of(1L))).thenReturn(List.of(contrato));
        doNothing().when(adminService).validarPasswordAdmin("admin", "admin123");
        doNothing().when(cronogramaContratoRepository).deleteByClienteIdIn(any());
        doNothing().when(clienteRepository).deleteAll(any());
        doNothing().when(registroAuditoriaService).registrarAccion(
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any());

        liberacionService.liberarLote(1L, "motivo", "admin123", "admin");

        verify(adminService).validarPasswordAdmin("admin", "admin123");
    }

    // ─── helper ──────────────────────────────────────────────────────

    private void setField(Object obj, String name, Object value) {
        try {
            var f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception ignored) {}
    }
}
