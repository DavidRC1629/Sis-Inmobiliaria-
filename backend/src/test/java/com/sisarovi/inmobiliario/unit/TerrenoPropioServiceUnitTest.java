package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.dto.TerrenoPropioRequest;
import com.sisarovi.inmobiliario.dto.TerrenoPropioResponse;
import com.sisarovi.inmobiliario.entity.Cliente;
import com.sisarovi.inmobiliario.entity.TerrenoPropio;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.repository.TerrenoPropioRepository;
import com.sisarovi.inmobiliario.service.CronogramaService;
import com.sisarovi.inmobiliario.service.RegistroAuditoriaService;
import com.sisarovi.inmobiliario.service.TerrenoPropioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerrenoPropioServiceUnitTest {

    @Mock private TerrenoPropioRepository terrenoPropioRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private RegistroAuditoriaService registroAuditoriaService;
    @Mock private CronogramaService cronogramaService;

    // Instanciamos manualmente para poder inyectar los @Autowired
    private TerrenoPropioService terrenoPropioService;

    private TerrenoPropio terreno;
    private Cliente propietario;

    @BeforeEach
    void setUp() {
        // TerrenoPropioService usa @RequiredArgsConstructor para los 2 repos
        // y @Autowired para registroAuditoriaService y cronogramaService
        terrenoPropioService = new TerrenoPropioService(terrenoPropioRepository, clienteRepository);
        ReflectionTestUtils.setField(terrenoPropioService, "registroAuditoriaService", registroAuditoriaService);
        ReflectionTestUtils.setField(terrenoPropioService, "cronogramaService", cronogramaService);

        propietario = Cliente.builder()
                .nombres("Carlos")
                .apellidos("Ruiz")
                .dni("12345678")
                .telefono("999888777")
                .direccion("Av. Principal")
                .tipoRelacion("ADQUISICION")
                .build();
        setField(propietario, "id", 1L);

        terreno = TerrenoPropio.builder()
                .numeroLote(5)
                .calle("Jr. Los Rosales")
                .areaM2(new BigDecimal("120.00"))
                .perimetro(new BigDecimal("44.00"))
                .medidaFrente(new BigDecimal("10.00"))
                .medidaFondo(new BigDecimal("12.00"))
                .medidaIzquierda(new BigDecimal("11.00"))
                .medidaDerecha(new BigDecimal("11.00"))
                .numeroPartida("12345678")
                .precio(new BigDecimal("50000.00"))
                .propietario(propietario)
                .estado("DISPONIBLE")
                .build();
        setField(terreno, "id", 1L);
    }

    // ─── getAll ─────────────────────────────────────────────────────────────────

    @Test
    void getAll_returnsMappedList() {
        when(terrenoPropioRepository.findAll()).thenReturn(List.of(terreno));

        List<TerrenoPropioResponse> result = terrenoPropioService.getAll();

        assertEquals(1, result.size());
        assertEquals("12345678", result.get(0).getNumeroPartida());
    }

    @Test
    void getAll_empty_returnsEmptyList() {
        when(terrenoPropioRepository.findAll()).thenReturn(List.of());

        assertTrue(terrenoPropioService.getAll().isEmpty());
    }

    // ─── getById ────────────────────────────────────────────────────────────────

    @Test
    void getById_existing_returnsOptionalPresent() {
        when(terrenoPropioRepository.findById(1L)).thenReturn(Optional.of(terreno));

        var result = terrenoPropioService.getById(1L);

        assertTrue(result.isPresent());
        assertEquals("12345678", result.get().getNumeroPartida());
    }

    @Test
    void getById_notFound_returnsEmpty() {
        when(terrenoPropioRepository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(terrenoPropioService.getById(99L).isEmpty());
    }

    // ─── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_validRequest_savesAndReturns() {
        TerrenoPropioRequest request = buildRequest("87654321", 1L);

        when(terrenoPropioRepository.existsByNumeroPartida("87654321")).thenReturn(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(propietario));
        when(terrenoPropioRepository.save(any(TerrenoPropio.class))).thenAnswer(inv -> inv.getArgument(0));

        TerrenoPropioResponse result = terrenoPropioService.create(request);

        assertNotNull(result);
        assertEquals("87654321", result.getNumeroPartida());
        verify(terrenoPropioRepository).save(any());
    }

    @Test
    void create_duplicatePartida_throwsException() {
        TerrenoPropioRequest request = buildRequest("12345678", 1L);

        when(terrenoPropioRepository.existsByNumeroPartida("12345678")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> terrenoPropioService.create(request));
        verify(terrenoPropioRepository, never()).save(any());
    }

    @Test
    void create_propietarioNotFound_throwsException() {
        TerrenoPropioRequest request = buildRequest("11111111", 99L);

        when(terrenoPropioRepository.existsByNumeroPartida("11111111")).thenReturn(false);
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> terrenoPropioService.create(request));
    }

    @Test
    void create_emptyPartida_throwsException() {
        TerrenoPropioRequest request = buildRequest("", 1L);

        assertThrows(IllegalArgumentException.class, () -> terrenoPropioService.create(request));
    }

    @Test
    void create_partida_tooLong_throwsException() {
        TerrenoPropioRequest request = buildRequest("123456789", 1L); // 9 digits > 8 max

        assertThrows(IllegalArgumentException.class, () -> terrenoPropioService.create(request));
    }

    @Test
    void create_partida_withLetters_throwsException() {
        TerrenoPropioRequest request = buildRequest("ABC123", 1L);

        assertThrows(IllegalArgumentException.class, () -> terrenoPropioService.create(request));
    }

    // ─── existsByNumeroPartida ──────────────────────────────────────────────────

    @Test
    void existsByNumeroPartida_validPartida_existsInRepo_returnsTrue() {
        when(terrenoPropioRepository.existsByNumeroPartida("12345678")).thenReturn(true);

        assertTrue(terrenoPropioService.existsByNumeroPartida("12345678"));
    }

    @Test
    void existsByNumeroPartida_invalidFormat_returnsFalse() {
        // Has letters → format check fails, should return false without hitting repo
        assertFalse(terrenoPropioService.existsByNumeroPartida("ABCD1234"));
        verify(terrenoPropioRepository, never()).existsByNumeroPartida(any());
    }

    @Test
    void existsByNumeroPartida_tooLong_returnsFalse() {
        assertFalse(terrenoPropioService.existsByNumeroPartida("123456789"));
        verify(terrenoPropioRepository, never()).existsByNumeroPartida(any());
    }

    // ─── adquirirTerreno ────────────────────────────────────────────────────────

    @Test
    void adquirirTerreno_disponible_setsVendidoAndRegisters() {
        when(terrenoPropioRepository.findById(1L)).thenReturn(Optional.of(terreno));
        when(terrenoPropioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(registroAuditoriaService).registrarAccion(any(), any(), any());
        doNothing().when(cronogramaService).crearCronogramaParaTerrenoPropio(any(), any(), any(), anyInt(), anyDouble());

        terrenoPropioService.adquirirTerreno(1L, 1L, "CONTADO", 1, 0.0);

        verify(terrenoPropioRepository).save(argThat(t -> "VENDIDO".equals(t.getEstado())));
        verify(registroAuditoriaService).registrarAccion(any(), eq("INGRESO"), any());
        verify(cronogramaService).crearCronogramaParaTerrenoPropio(any(), any(), any(), anyInt(), anyDouble());
    }

    @Test
    void adquirirTerreno_noDisponible_throwsException() {
        terreno.setEstado("VENDIDO");
        when(terrenoPropioRepository.findById(1L)).thenReturn(Optional.of(terreno));

        assertThrows(IllegalStateException.class,
                () -> terrenoPropioService.adquirirTerreno(1L, 1L, "CONTADO", 1, 0.0));
        verify(terrenoPropioRepository, never()).save(any());
    }

    @Test
    void adquirirTerreno_notFound_throwsException() {
        when(terrenoPropioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> terrenoPropioService.adquirirTerreno(99L, 1L, "CONTADO", 1, 0.0));
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private TerrenoPropioRequest buildRequest(String numeroPartida, Long propietarioId) {
        TerrenoPropioRequest req = new TerrenoPropioRequest();
        req.setNumeroLote(1);
        req.setCalle("Av. Test");
        req.setAreaM2(new BigDecimal("100.00"));
        req.setPerimetro(new BigDecimal("40.00"));
        req.setMedidaFrente(new BigDecimal("10.00"));
        req.setMedidaFondo(new BigDecimal("10.00"));
        req.setMedidaIzquierda(new BigDecimal("10.00"));
        req.setMedidaDerecha(new BigDecimal("10.00"));
        req.setNumeroPartida(numeroPartida);
        req.setPrecio(new BigDecimal("30000.00"));
        req.setPropietarioId(propietarioId);
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
