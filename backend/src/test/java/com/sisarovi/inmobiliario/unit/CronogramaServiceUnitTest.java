package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.dto.*;
import com.sisarovi.inmobiliario.entity.*;
import com.sisarovi.inmobiliario.repository.*;
import com.sisarovi.inmobiliario.service.CronogramaService;
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
class CronogramaServiceUnitTest {

    @Mock CronogramaContratoRepository contratoRepository;
    @Mock CronogramaCuotaRepository cuotaRepository;
    @Mock CronogramaPagoSeparacionRepository pagoSeparacionRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock RegistroAuditoriaService registroAuditoriaService;

    @InjectMocks CronogramaService cronogramaService;

    private Lote lote;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        doNothing().when(registroAuditoriaService).registrarAccion(any(), any(), any());
        doNothing().when(registroAuditoriaService).registrarAccion(
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any());

        lote = buildLote();
        cliente = buildCliente(lote);
    }

    // ════════════════════════════════════════════════════════════════════
    // crearDesdeAdquisicion — CONTADO
    // ════════════════════════════════════════════════════════════════════

    @Test
    void crearDesdeAdquisicion_contado_pagadoCompleto_estadoAlDia() {
        ClienteAdquisicionRequest req = buildRequest("CONTADO", new BigDecimal("50000"),
                new BigDecimal("50000"), 1, BigDecimal.ZERO);

        when(contratoRepository.save(any())).thenAnswer(inv -> {
            CronogramaContrato c = inv.getArgument(0);
            setField(c, "id", 1L);
            return c;
        });
        when(pagoSeparacionRepository.findByContratoIdOrderByIdAsc(any())).thenReturn(List.of());
        when(clienteRepository.findAllByLoteId(any())).thenReturn(List.of(cliente));

        cronogramaService.crearDesdeAdquisicion(cliente, req);

        verify(contratoRepository, atLeastOnce()).save(argThat(c ->
                "AL_DIA".equals(c.getEstado())));
    }

    @Test
    void crearDesdeAdquisicion_credito_saldoConInteres_estadoSeparacionEnCurso() {
        // CREDITO con monto < separacion objetivo → queda SEPARACION_EN_CURSO
        ClienteAdquisicionRequest req = buildRequest("CREDITO", new BigDecimal("50000"),
                new BigDecimal("500"), 24, new BigDecimal("10"));

        when(contratoRepository.save(any())).thenAnswer(inv -> {
            CronogramaContrato c = inv.getArgument(0);
            setField(c, "id", 2L);
            c.setCuotas(new ArrayList<>());
            return c;
        });

        cronogramaService.crearDesdeAdquisicion(cliente, req);

        verify(contratoRepository).save(argThat(c ->
                "SEPARACION_EN_CURSO".equals(c.getEstado())));
    }

    @Test
    void crearDesdeAdquisicion_tipoInvalido_throws() {
        ClienteAdquisicionRequest req = buildRequest("INVALIDO", new BigDecimal("50000"),
                new BigDecimal("10000"), 12, BigDecimal.ZERO);

        assertThrows(RuntimeException.class,
                () -> cronogramaService.crearDesdeAdquisicion(cliente, req));
    }

    @Test
    void crearDesdeAdquisicion_precioVentaCero_throws() {
        ClienteAdquisicionRequest req = buildRequest("CREDITO", BigDecimal.ZERO,
                new BigDecimal("2000"), 12, BigDecimal.ZERO);

        assertThrows(RuntimeException.class,
                () -> cronogramaService.crearDesdeAdquisicion(cliente, req));
    }

    @Test
    void crearDesdeAdquisicion_montoOperacionMenor100_throws() {
        ClienteAdquisicionRequest req = buildRequest("SEPARACION", new BigDecimal("50000"),
                new BigDecimal("50"), 12, BigDecimal.ZERO);

        assertThrows(RuntimeException.class,
                () -> cronogramaService.crearDesdeAdquisicion(cliente, req));
    }

    @Test
    void crearDesdeAdquisicion_contado_montoDistintoPrecio_throws() {
        ClienteAdquisicionRequest req = buildRequest("CONTADO", new BigDecimal("50000"),
                new BigDecimal("40000"), 1, BigDecimal.ZERO);

        assertThrows(RuntimeException.class,
                () -> cronogramaService.crearDesdeAdquisicion(cliente, req));
    }

    // ════════════════════════════════════════════════════════════════════
    // crearDesdeAdquisicion — SEPARACION (luego CREDITO)
    // ════════════════════════════════════════════════════════════════════

    @Test
    void crearDesdeAdquisicion_separacion_separacionEnCurso() {
        // montoOperacion < montoSeparacionObjetivo → SEPARACION_EN_CURSO
        ClienteAdquisicionRequest req = buildRequest("SEPARACION", new BigDecimal("50000"),
                new BigDecimal("500"), 24, new BigDecimal("10"));

        when(contratoRepository.save(any())).thenAnswer(inv -> {
            CronogramaContrato c = inv.getArgument(0);
            setField(c, "id", 3L);
            c.setCuotas(new ArrayList<>());
            return c;
        });

        cronogramaService.crearDesdeAdquisicion(cliente, req);

        verify(contratoRepository).save(argThat(c ->
                "SEPARACION_EN_CURSO".equals(c.getEstado())));
    }

    @Test
    void crearDesdeAdquisicion_credito_separacionCompletada_activaCronograma() {
        // monto >= DEFAULT_SEPARACION_OBJETIVO (2000) → separación completa → activa cuotas
        ClienteAdquisicionRequest req = buildRequest("CREDITO", new BigDecimal("50000"),
                new BigDecimal("5000"), 24, new BigDecimal("10"));

        when(contratoRepository.save(any())).thenAnswer(inv -> {
            CronogramaContrato c = inv.getArgument(0);
            setField(c, "id", 4L);
            if (c.getCuotas() == null) c.setCuotas(new ArrayList<>());
            if (c.getPagosSeparacion() == null) c.setPagosSeparacion(new ArrayList<>());
            return c;
        });
        when(cuotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pagoSeparacionRepository.findByContratoIdOrderByIdAsc(any())).thenReturn(List.of());
        when(clienteRepository.findAllByLoteId(any())).thenReturn(List.of(cliente));

        cronogramaService.crearDesdeAdquisicion(cliente, req);

        // El contrato final debe estar en estado AL_DIA o DEUDOR (no SEPARACION_EN_CURSO)
        verify(contratoRepository, atLeastOnce()).save(argThat(c ->
                !"SEPARACION_EN_CURSO".equals(c.getEstado())));
    }

    // ════════════════════════════════════════════════════════════════════
    // registrarPagoSeparacion
    // ════════════════════════════════════════════════════════════════════

    @Test
    void registrarPagoSeparacion_montoValido_actualiza() {
        CronogramaContrato contrato = buildContratoSeparacionEnCurso(1L);
        when(contratoRepository.findById(1L)).thenReturn(Optional.of(contrato));
        when(contratoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pagoSeparacionRepository.findByContratoIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(clienteRepository.findAllByLoteId(any())).thenReturn(List.of(cliente));

        RegistrarPagoRequest req = new RegistrarPagoRequest();
        req.setMonto(new BigDecimal("500"));
        req.setFechaPago(LocalDate.now());

        CronogramaContratoResponse result = cronogramaService.registrarPagoSeparacion(1L, req);

        assertNotNull(result);
        verify(contratoRepository).save(any());
    }

    @Test
    void registrarPagoSeparacion_montoCero_throws() {
        CronogramaContrato contrato = buildContratoSeparacionEnCurso(2L);
        when(contratoRepository.findById(2L)).thenReturn(Optional.of(contrato));

        RegistrarPagoRequest req = new RegistrarPagoRequest();
        req.setMonto(BigDecimal.ZERO);

        assertThrows(RuntimeException.class,
                () -> cronogramaService.registrarPagoSeparacion(2L, req));
    }

    @Test
    void registrarPagoSeparacion_contratoNoEnSeparacion_throws() {
        CronogramaContrato contrato = buildContratoSeparacionEnCurso(3L);
        contrato.setEstado("AL_DIA"); // ya no está en separación

        when(contratoRepository.findById(3L)).thenReturn(Optional.of(contrato));

        RegistrarPagoRequest req = new RegistrarPagoRequest();
        req.setMonto(new BigDecimal("500"));

        assertThrows(RuntimeException.class,
                () -> cronogramaService.registrarPagoSeparacion(3L, req));
    }

    @Test
    void registrarPagoSeparacion_completaSeparacion_activaCronograma() {
        // Pago que completa la separación (2000 objetivo, ya pagó 1500, paga 500 más)
        CronogramaContrato contrato = buildContratoSeparacionEnCurso(4L);
        contrato.setMontoPagadoTotal(new BigDecimal("1500"));
        contrato.setMontoSeparacionAcumulado(new BigDecimal("1500"));

        when(contratoRepository.findById(4L)).thenReturn(Optional.of(contrato));
        when(contratoRepository.save(any())).thenAnswer(inv -> {
            CronogramaContrato c = inv.getArgument(0);
            setField(c, "id", 4L);
            c.setCuotas(new ArrayList<>());
            return c;
        });
        when(cuotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pagoSeparacionRepository.findByContratoIdOrderByIdAsc(4L)).thenReturn(List.of());
        when(clienteRepository.findAllByLoteId(any())).thenReturn(List.of(cliente));

        RegistrarPagoRequest req = new RegistrarPagoRequest();
        req.setMonto(new BigDecimal("500")); // completa los 2000
        req.setFechaPago(LocalDate.now());

        CronogramaContratoResponse result = cronogramaService.registrarPagoSeparacion(4L, req);

        assertNotNull(result);
    }

    @Test
    void registrarPagoSeparacion_notFound_throws() {
        when(contratoRepository.findById(99L)).thenReturn(Optional.empty());

        RegistrarPagoRequest req = new RegistrarPagoRequest();
        req.setMonto(new BigDecimal("500"));

        assertThrows(RuntimeException.class,
                () -> cronogramaService.registrarPagoSeparacion(99L, req));
    }

    // ════════════════════════════════════════════════════════════════════
    // registrarPagoCuota
    // ════════════════════════════════════════════════════════════════════

    @Test
    void registrarPagoCuota_valido_actualiza() {
        CronogramaContrato contrato = buildContratoConCuotas(5L);
        CronogramaCuota cuota = contrato.getCuotas().get(0);
        // La lógica redondea el montoCuota a decenas: 1833 → 1830
        // Para que quede PAGADA, el monto debe cubrir exactamente lo que el servicio calcula
        // Usamos un monto de cuota exacto (2000 → no redondea, es múltiplo de 10)
        cuota.setMontoCuota(new BigDecimal("2000"));
        setField(cuota, "id", 1L);

        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(cuota));
        when(contratoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cuotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pagoSeparacionRepository.findByContratoIdOrderByIdAsc(5L)).thenReturn(List.of());
        when(clienteRepository.findAllByLoteId(any())).thenReturn(List.of(cliente));

        RegistrarPagoRequest req = new RegistrarPagoRequest();
        req.setMonto(new BigDecimal("2000")); // paga exactamente el monto de la cuota
        req.setFechaPago(LocalDate.now());

        CronogramaContratoResponse result = cronogramaService.registrarPagoCuota(1L, req);

        assertNotNull(result);
        assertEquals("PAGADA", cuota.getEstadoPago());
    }

    @Test
    void registrarPagoCuota_cuotaYaPagada_throws() {
        CronogramaContrato contrato = buildContratoConCuotas(6L);
        CronogramaCuota cuota = contrato.getCuotas().get(0);
        cuota.setMontoPagado(cuota.getMontoCuota()); // ya pagada
        cuota.setEstadoPago("PAGADA");
        setField(cuota, "id", 2L);

        when(cuotaRepository.findById(2L)).thenReturn(Optional.of(cuota));

        RegistrarPagoRequest req = new RegistrarPagoRequest();
        req.setMonto(new BigDecimal("500"));

        assertThrows(RuntimeException.class,
                () -> cronogramaService.registrarPagoCuota(2L, req));
    }

    @Test
    void registrarPagoCuota_montoMayorSaldo_aplicaHastaSaldo() {
        CronogramaContrato contrato = buildContratoConCuotas(7L);
        CronogramaCuota cuota = contrato.getCuotas().get(0);
        setField(cuota, "id", 3L);

        when(cuotaRepository.findById(3L)).thenReturn(Optional.of(cuota));
        when(contratoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cuotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pagoSeparacionRepository.findByContratoIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(clienteRepository.findAllByLoteId(any())).thenReturn(List.of(cliente));

        RegistrarPagoRequest req = new RegistrarPagoRequest();
        req.setMonto(new BigDecimal("9999")); // mucho más que el saldo
        req.setFechaPago(LocalDate.now());

        CronogramaContratoResponse result = cronogramaService.registrarPagoCuota(3L, req);

        assertNotNull(result);
        assertEquals("PAGADA", cuota.getEstadoPago()); // se pagó exactamente el saldo
    }

    @Test
    void registrarPagoCuota_cuotaAnteriorPendiente_throws() {
        CronogramaContrato contrato = buildContratoConCuotas(8L);
        // Cuota 2 intentada sin pagar cuota 1
        CronogramaCuota cuota2 = new CronogramaCuota();
        cuota2.setNumeroCuota(2);
        cuota2.setMontoCuota(new BigDecimal("1500"));
        cuota2.setMontoPagado(BigDecimal.ZERO);
        cuota2.setEstadoPago("PENDIENTE");
        cuota2.setFechaVencimiento(LocalDate.now().plusMonths(2));
        cuota2.setPagos(new ArrayList<>());
        cuota2.setContrato(contrato);
        setField(cuota2, "id", 4L);
        contrato.getCuotas().add(cuota2);

        when(cuotaRepository.findById(4L)).thenReturn(Optional.of(cuota2));

        RegistrarPagoRequest req = new RegistrarPagoRequest();
        req.setMonto(new BigDecimal("1500"));

        assertThrows(RuntimeException.class,
                () -> cronogramaService.registrarPagoCuota(4L, req));
    }

    @Test
    void registrarPagoCuota_notFound_throws() {
        when(cuotaRepository.findById(99L)).thenReturn(Optional.empty());

        RegistrarPagoRequest req = new RegistrarPagoRequest();
        req.setMonto(new BigDecimal("1000"));

        assertThrows(RuntimeException.class,
                () -> cronogramaService.registrarPagoCuota(99L, req));
    }

    // ════════════════════════════════════════════════════════════════════
    // obtenerPorId / actualizarAsesor / listar
    // ════════════════════════════════════════════════════════════════════

    @Test
    void obtenerPorId_existing_returnsResponse() {
        CronogramaContrato contrato = buildContratoConCuotas(10L);
        when(contratoRepository.findById(10L)).thenReturn(Optional.of(contrato));
        when(contratoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pagoSeparacionRepository.findByContratoIdOrderByIdAsc(10L)).thenReturn(List.of());
        when(clienteRepository.findAllByLoteId(any())).thenReturn(List.of(cliente));

        CronogramaContratoResponse result = cronogramaService.obtenerPorId(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    void obtenerPorId_notFound_throws() {
        when(contratoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cronogramaService.obtenerPorId(99L));
    }

    @Test
    void actualizarAsesor_valid_updatesAsesor() {
        CronogramaContrato contrato = buildContratoConCuotas(11L);
        when(contratoRepository.findById(11L)).thenReturn(Optional.of(contrato));
        when(contratoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pagoSeparacionRepository.findByContratoIdOrderByIdAsc(11L)).thenReturn(List.of());
        when(clienteRepository.findAllByLoteId(any())).thenReturn(List.of(cliente));

        CronogramaContratoResponse result = cronogramaService.actualizarAsesor(11L, "Nuevo Asesor");

        assertNotNull(result);
        verify(contratoRepository).save(argThat(c -> "Nuevo Asesor".equals(c.getAsesor())));
    }

    @Test
    void actualizarAsesor_notFound_throws() {
        when(contratoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cronogramaService.actualizarAsesor(99L, "X"));
    }

    @Test
    void listar_sinFiltro_returnsList() {
        CronogramaContrato contrato = buildContratoConCuotas(12L);
        when(contratoRepository.findAllDetailed()).thenReturn(List.of(contrato));
        when(contratoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pagoSeparacionRepository.findByContratoIdOrderByIdAsc(12L)).thenReturn(List.of());
        when(clienteRepository.findAllByLoteId(any())).thenReturn(List.of(cliente));

        CronogramaFilterRequest filter = new CronogramaFilterRequest();
        List<CronogramaContratoResponse> result = cronogramaService.listar(filter);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    // ════════════════════════════════════════════════════════════════════
    // aplicarDescuento
    // ════════════════════════════════════════════════════════════════════

    @Test
    void aplicarDescuento_montoMayorCero_aplicaDescuento() {
        // Construir contrato con cliente de id=1L explícitamente
        CronogramaContrato contrato = buildContratoConCuotas(13L);
        // Asegurar que el estado no sea SEPARACION_EN_CURSO para que pase el filtro
        contrato.setEstado("AL_DIA");

        when(contratoRepository.findAll()).thenReturn(List.of(contrato));
        when(contratoRepository.findById(13L)).thenReturn(Optional.of(contrato));
        when(contratoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cuotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pagoSeparacionRepository.findByContratoIdOrderByIdAsc(13L)).thenReturn(List.of());
        when(clienteRepository.findAllByLoteId(any())).thenReturn(List.of(cliente));

        CronogramaDescuentoRequest req = new CronogramaDescuentoRequest();
        req.setClienteId(1L); // debe coincidir con cliente.getId() en buildContratoConCuotas
        req.setMontoDescuento(new BigDecimal("500"));
        req.setObservacion("Descuento test");

        CronogramaContratoResponse result = cronogramaService.aplicarDescuento(req);

        assertNotNull(result);
        verify(contratoRepository, atLeastOnce()).save(any());
    }

    @Test
    void aplicarDescuento_montoZero_throws() {
        CronogramaDescuentoRequest req = new CronogramaDescuentoRequest();
        req.setClienteId(1L);
        req.setMontoDescuento(BigDecimal.ZERO);

        assertThrows(RuntimeException.class, () -> cronogramaService.aplicarDescuento(req));
    }

    @Test
    void aplicarDescuento_sinCronogramas_throws() {
        when(contratoRepository.findAll()).thenReturn(List.of());

        CronogramaDescuentoRequest req = new CronogramaDescuentoRequest();
        req.setClienteId(99L);
        req.setMontoDescuento(new BigDecimal("500"));

        assertThrows(RuntimeException.class, () -> cronogramaService.aplicarDescuento(req));
    }

    // ════════════════════════════════════════════════════════════════════
    // crearCronogramaParaTerrenoPropio
    // ════════════════════════════════════════════════════════════════════

    @Test
    void crearCronogramaParaTerrenoPropio_propietarioNotFound_throws() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        TerrenoPropio terreno = TerrenoPropio.builder()
                .numeroLote(1).calle("Av.").areaM2(new BigDecimal("100"))
                .perimetro(new BigDecimal("40")).medidaFrente(new BigDecimal("10"))
                .medidaFondo(new BigDecimal("10")).medidaIzquierda(new BigDecimal("10"))
                .medidaDerecha(new BigDecimal("10")).numeroPartida("12345678")
                .precio(new BigDecimal("30000")).estado("DISPONIBLE").build();

        assertThrows(RuntimeException.class, () ->
                cronogramaService.crearCronogramaParaTerrenoPropio(terreno, 99L, "CONTADO", 1, 0.0));
    }

    // ════════════════════════════════════════════════════════════════════
    // Builders
    // ════════════════════════════════════════════════════════════════════

    private ClienteAdquisicionRequest buildRequest(String tipo, BigDecimal precio,
                                                    BigDecimal monto, int plazo, BigDecimal interes) {
        ClienteAdquisicionRequest req = new ClienteAdquisicionRequest();
        req.setTipoOperacion(tipo);
        req.setPrecioVenta(precio);
        req.setMontoOperacion(monto);
        req.setAsesor("Asesor");
        req.setPlazoMeses(plazo);
        req.setInteresPorcentaje(interes);
        req.setFechaOperacion(LocalDate.now());
        return req;
    }

    private CronogramaContrato buildContratoSeparacionEnCurso(Long id) {
        CronogramaContrato c = CronogramaContrato.builder()
                .cliente(cliente)
                .tipoOperacion("SEPARACION")
                .estado("SEPARACION_EN_CURSO")
                .fechaOperacion(LocalDate.now())
                .precioVenta(new BigDecimal("50000"))
                .montoPagadoTotal(new BigDecimal("500"))
                .montoSeparacionObjetivo(new BigDecimal("2000"))
                .montoSeparacionAcumulado(new BigDecimal("500"))
                .saldoFinanciarInicial(BigDecimal.ZERO)
                .plazoMeses(24)
                .interesPorcentaje(new BigDecimal("10"))
                .montoCuotaReferencial(BigDecimal.ZERO)
                .cuotas(new ArrayList<>())
                .pagosSeparacion(new ArrayList<>())
                .build();
        setField(c, "id", id);
        return c;
    }

    private CronogramaContrato buildContratoConCuotas(Long id) {
        CronogramaContrato c = CronogramaContrato.builder()
                .cliente(cliente)
                .tipoOperacion("CREDITO")
                .estado("AL_DIA")
                .fechaOperacion(LocalDate.now())
                .precioVenta(new BigDecimal("50000"))
                .montoPagadoTotal(new BigDecimal("10000"))
                .montoSeparacionObjetivo(new BigDecimal("2000"))
                .montoSeparacionAcumulado(new BigDecimal("2000"))
                .saldoFinanciarInicial(new BigDecimal("40000"))
                .plazoMeses(24)
                .interesPorcentaje(new BigDecimal("10"))
                .montoCuotaReferencial(new BigDecimal("1833"))
                .cuotas(new ArrayList<>())
                .pagosSeparacion(new ArrayList<>())
                .build();
        setField(c, "id", id);

        CronogramaCuota cuota = new CronogramaCuota();
        cuota.setNumeroCuota(1);
        cuota.setMontoCuota(new BigDecimal("1833"));
        cuota.setMontoPagado(BigDecimal.ZERO);
        cuota.setEstadoPago("PENDIENTE");
        cuota.setFechaVencimiento(LocalDate.now().plusMonths(1));
        cuota.setPagos(new ArrayList<>());
        cuota.setContrato(c);
        c.getCuotas().add(cuota);
        return c;
    }

    private Lote buildLote() {
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

        Lote l = new Lote();
        setField(l, "id", 1L);
        l.setNumero(1);
        l.setNumeroPartida("12345678");
        l.setPrecioLote(new BigDecimal("50000"));
        l.setParcela(parcela);
        l.setManzana(manzana);
        return l;
    }

    private Cliente buildCliente(Lote lote) {
        Cliente c = Cliente.builder()
                .nombres("Juan").apellidos("Perez").dni("12345678")
                .email("j@t.com").telefono("999").direccion("Av.")
                .tipoRelacion("ADQUISICION").clienteDesde(LocalDate.now()).lote(lote)
                .build();
        setField(c, "id", 1L);
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
