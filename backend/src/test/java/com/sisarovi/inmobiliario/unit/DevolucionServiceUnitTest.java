package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.dto.DevolucionPagoRequest;
import com.sisarovi.inmobiliario.dto.DevolucionRequest;
import com.sisarovi.inmobiliario.dto.DevolucionResponse;
import com.sisarovi.inmobiliario.entity.Devolucion;
import com.sisarovi.inmobiliario.entity.DevolucionPago;
import com.sisarovi.inmobiliario.repository.DevolucionPagoRepository;
import com.sisarovi.inmobiliario.repository.DevolucionRepository;
import com.sisarovi.inmobiliario.service.DevolucionService;
import com.sisarovi.inmobiliario.service.RegistroAuditoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevolucionServiceUnitTest {

    @Mock private DevolucionRepository devolucionRepository;
    @Mock private DevolucionPagoRepository devolucionPagoRepository;
    @Mock private RegistroAuditoriaService registroAuditoriaService;

    @InjectMocks
    private DevolucionService devolucionService;

    private Devolucion devolucionBase;

    @BeforeEach
    void setUp() {
        devolucionBase = buildDevolucion(1L, "EN_CURSO",
                new BigDecimal("1000"), BigDecimal.ZERO);
    }

    // ─── listar ─────────────────────────────────────────────────────────────

    @Test
    void listar_noEstado_returnsAllOrdered() {
        when(devolucionRepository.findAllByOrderByFechaCreacionDesc())
                .thenReturn(List.of(devolucionBase));
        when(devolucionPagoRepository.findByDevolucionIdOrderByFechaRegistroDesc(1L))
                .thenReturn(List.of());

        List<DevolucionResponse> result = devolucionService.listar(null);

        assertEquals(1, result.size());
        assertEquals("EN_CURSO", result.get(0).getEstado());
    }

    @Test
    void listar_withEstado_filtersCorrectly() {
        when(devolucionRepository.findByEstadoIgnoreCaseOrderByFechaCreacionDesc("COMPLETADA"))
                .thenReturn(List.of());

        List<DevolucionResponse> result = devolucionService.listar("COMPLETADA");

        assertTrue(result.isEmpty());
    }

    @Test
    void listar_blankEstado_returnsAll() {
        when(devolucionRepository.findAllByOrderByFechaCreacionDesc())
                .thenReturn(List.of());

        List<DevolucionResponse> result = devolucionService.listar("  ");

        assertTrue(result.isEmpty());
    }

    // ─── obtener ────────────────────────────────────────────────────────────

    @Test
    void obtener_existingId_returnsResponse() {
        when(devolucionRepository.findById(1L)).thenReturn(Optional.of(devolucionBase));
        when(devolucionPagoRepository.findByDevolucionIdOrderByFechaRegistroDesc(1L))
                .thenReturn(List.of());

        DevolucionResponse result = devolucionService.obtener(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void obtener_notFound_throwsException() {
        when(devolucionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> devolucionService.obtener(99L));
    }

    // ─── crear ──────────────────────────────────────────────────────────────

    @Test
    void crear_validRequest_savesDevolucion() {
        DevolucionRequest request = buildRequest(new BigDecimal("500"));

        when(devolucionRepository.save(any(Devolucion.class))).thenAnswer(inv -> {
            Devolucion d = inv.getArgument(0);
            setField(d, "id", 2L);
            return d;
        });
        doNothing().when(registroAuditoriaService).registrarAccion(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        when(devolucionPagoRepository.findByDevolucionIdOrderByFechaRegistroDesc(any()))
                .thenReturn(List.of());

        DevolucionResponse result = devolucionService.crear(request, "admin");

        assertNotNull(result);
        assertEquals("EN_CURSO", result.getEstado());
        assertEquals(new BigDecimal("500"), result.getMontoTotal());
        verify(devolucionRepository).save(any());
    }

    @Test
    void crear_nullMonto_setsZero() {
        DevolucionRequest request = buildRequest(null);

        when(devolucionRepository.save(any(Devolucion.class))).thenAnswer(inv -> {
            Devolucion d = inv.getArgument(0);
            setField(d, "id", 3L);
            return d;
        });
        doNothing().when(registroAuditoriaService).registrarAccion(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        when(devolucionPagoRepository.findByDevolucionIdOrderByFechaRegistroDesc(any()))
                .thenReturn(List.of());

        DevolucionResponse result = devolucionService.crear(request, "admin");

        assertEquals(BigDecimal.ZERO, result.getMontoTotal());
    }

    // ─── registrarPago ──────────────────────────────────────────────────────

    @Test
    void registrarPago_parcial_setsEnCursoAndAccumulates() {
        devolucionBase.setPagos(new ArrayList<>());

        DevolucionPagoRequest request = new DevolucionPagoRequest();
        request.setMonto(new BigDecimal("300"));
        request.setFechaPago(LocalDate.now());
        request.setDescripcion("Pago parcial");
        request.setMedioPago("EFECTIVO");

        when(devolucionRepository.findById(1L)).thenReturn(Optional.of(devolucionBase));
        when(devolucionPagoRepository.save(any(DevolucionPago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(devolucionRepository.save(any(Devolucion.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(registroAuditoriaService).registrarAccion(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        when(devolucionPagoRepository.findByDevolucionIdOrderByFechaRegistroDesc(any()))
                .thenReturn(List.of());

        DevolucionResponse result = devolucionService.registrarPago(1L, request, "admin");

        assertEquals("EN_CURSO", result.getEstado());
        assertEquals(new BigDecimal("300"), result.getMontoPagado());
    }

    @Test
    void registrarPago_completaPago_setsCompletada() {
        devolucionBase.setPagos(new ArrayList<>());

        DevolucionPagoRequest request = new DevolucionPagoRequest();
        request.setMonto(new BigDecimal("1000")); // igual al total
        request.setFechaPago(LocalDate.now());
        request.setDescripcion("Pago final");
        request.setMedioPago("TRANSFERENCIA");

        when(devolucionRepository.findById(1L)).thenReturn(Optional.of(devolucionBase));
        when(devolucionPagoRepository.save(any(DevolucionPago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(devolucionRepository.save(any(Devolucion.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(registroAuditoriaService).registrarAccion(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        when(devolucionPagoRepository.findByDevolucionIdOrderByFechaRegistroDesc(any()))
                .thenReturn(List.of());

        DevolucionResponse result = devolucionService.registrarPago(1L, request, "admin");

        assertEquals("COMPLETADA", result.getEstado());
        assertEquals(new BigDecimal("1000"), result.getMontoPagado());
    }

    @Test
    void registrarPago_devolucionNotFound_throwsException() {
        when(devolucionRepository.findById(99L)).thenReturn(Optional.empty());

        DevolucionPagoRequest request = new DevolucionPagoRequest();
        request.setMonto(new BigDecimal("100"));

        assertThrows(IllegalArgumentException.class,
                () -> devolucionService.registrarPago(99L, request, "admin"));
    }

    // ─── Response calculations (progreso, pendiente) ─────────────────────────

    @Test
    void obtener_withPagos_calculatesProgresoCorrectly() {
        Devolucion dev = buildDevolucion(5L, "EN_CURSO",
                new BigDecimal("200"), new BigDecimal("100"));

        DevolucionPago pago = DevolucionPago.builder()
                .monto(new BigDecimal("100"))
                .fechaPago(LocalDate.now())
                .descripcion("pago")
                .medioPago("EFECTIVO")
                .build();
        setField(pago, "id", 1L);

        when(devolucionRepository.findById(5L)).thenReturn(Optional.of(dev));
        when(devolucionPagoRepository.findByDevolucionIdOrderByFechaRegistroDesc(5L))
                .thenReturn(List.of(pago));

        DevolucionResponse result = devolucionService.obtener(5L);

        assertEquals(50, result.getProgreso());  // 100/200 = 50%
        assertEquals(new BigDecimal("100"), result.getMontoPendiente());
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private Devolucion buildDevolucion(Long id, String estado, BigDecimal total, BigDecimal pagado) {
        Devolucion d = Devolucion.builder()
                .loteId(10L)
                .loteNumero(1)
                .manzana("Manzana A")
                .parcelaNombre("Parcela 1")
                .etapaNumero(1)
                .proyectoNombre("Proyecto Test")
                .montoTotal(total)
                .montoPagado(pagado)
                .dias(30)
                .fechaInicio(LocalDate.now())
                .fechaFinEstimada(LocalDate.now().plusDays(30))
                .descripcion("Devolución de prueba")
                .estado(estado)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        d.setPagos(new ArrayList<>());
        setField(d, "id", id);
        return d;
    }

    private DevolucionRequest buildRequest(BigDecimal monto) {
        DevolucionRequest req = new DevolucionRequest();
        req.setLoteId(10L);
        req.setLoteNumero(1);
        req.setManzana("Manzana A");
        req.setParcelaNombre("Parcela 1");
        req.setEtapaNumero(1);
        req.setProyectoNombre("Proyecto Test");
        req.setMontoTotal(monto);
        req.setDias(30);
        req.setFechaInicio(LocalDate.now());
        req.setFechaFinEstimada(LocalDate.now().plusDays(30));
        req.setDescripcion("Devolución test");
        return req;
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            var f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception ignored) {}
    }
}
