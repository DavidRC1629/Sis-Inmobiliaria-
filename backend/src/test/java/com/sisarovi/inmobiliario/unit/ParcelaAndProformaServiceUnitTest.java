package com.sisarovi.inmobiliario.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.dto.ParcelaRequest;
import com.sisarovi.inmobiliario.dto.ParcelaResponse;
import com.sisarovi.inmobiliario.dto.ProformaRequest;
import com.sisarovi.inmobiliario.dto.ProformaResponse;
import com.sisarovi.inmobiliario.entity.*;
import com.sisarovi.inmobiliario.repository.*;
import com.sisarovi.inmobiliario.service.ParcelaService;
import com.sisarovi.inmobiliario.service.ProformaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParcelaAndProformaServiceUnitTest {

    // ══════════════════════════════════════════════════════════════════════
    // ParcelaService
    // ══════════════════════════════════════════════════════════════════════

    @Mock private ParcelaRepository parcelaRepository;
    @Mock private EtapaRepository etapaRepository;
    @Mock private LoteRepository loteRepository;
    @Mock private ManzanaRepository manzanaRepository;

    @InjectMocks private ParcelaService parcelaService;

    // ══════════════════════════════════════════════════════════════════════
    // ProformaService
    // ══════════════════════════════════════════════════════════════════════

    @Mock private ProformaRepository proformaRepository;

    private ProformaService proformaService;

    private Etapa etapa;
    private Parcela parcela;

    @BeforeEach
    void setUp() {
        proformaService = new ProformaService(proformaRepository, new ObjectMapper());

        Role role = Role.builder().name("ROLE_ADMIN").build();
        setField(role, "id", 1L);
        User user = User.builder().dni("admin").nombres("A").primerApellido("B").segundoApellido("")
                .password("pw").email("a@b.com").role(role).estado(UserStatus.ACTIVO).enabled(true).build();
        setField(user, "id", 1L);

        Project project = new Project();
        setField(project, "id", 1L);
        project.setNombre("Proj");
        project.setCantidadEtapas(1);
        project.setCreatedBy(user);
        project.setEtapas(new ArrayList<>());

        etapa = new Etapa();
        setField(etapa, "id", 1L);
        etapa.setNumeroEtapa(1);
        etapa.setProject(project);
        etapa.setParcelas(new ArrayList<>());

        parcela = new Parcela();
        setField(parcela, "id", 1L);
        parcela.setNombre("Parcela A");
        parcela.setNumManzanas(2);
        parcela.setNumLotes(0);
        parcela.setPropietario("Prop");
        parcela.setLotesDisponibles(0);
        parcela.setEtapa(etapa);
        parcela.setLotes(new ArrayList<>());
    }

    // ════════════════════════════════════════════════════════════
    // ParcelaService tests
    // ════════════════════════════════════════════════════════════

    @Test
    void getParcelasByEtapa_returnsList() {
        when(parcelaRepository.findByEtapaIdOrderByNombreAsc(1L)).thenReturn(List.of(parcela));

        List<ParcelaResponse> result = parcelaService.getParcelasByEtapa(1L);

        assertEquals(1, result.size());
        assertEquals("Parcela A", result.get(0).getNombre());
    }

    @Test
    void getParcelasByEtapa_empty_returnsEmpty() {
        when(parcelaRepository.findByEtapaIdOrderByNombreAsc(99L)).thenReturn(List.of());
        assertTrue(parcelaService.getParcelasByEtapa(99L).isEmpty());
    }

    @Test
    void getParcelaById_existing_returnsResponse() {
        when(parcelaRepository.findById(1L)).thenReturn(Optional.of(parcela));
        ParcelaResponse result = parcelaService.getParcelaById(1L);
        assertEquals("Parcela A", result.getNombre());
    }

    @Test
    void getParcelaById_notFound_throws() {
        when(parcelaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> parcelaService.getParcelaById(99L));
    }

    @Test
    void searchByPropietario_returnsList() {
        when(parcelaRepository.findByPropietarioContainingIgnoreCase("Prop"))
                .thenReturn(List.of(parcela));
        List<ParcelaResponse> result = parcelaService.searchByPropietario("Prop");
        assertEquals(1, result.size());
    }

    @Test
    void createParcela_valid_savesAndEnsuresManzanas() {
        ParcelaRequest req = new ParcelaRequest("Nueva", 2, "Owner");

        when(etapaRepository.findById(1L)).thenReturn(Optional.of(etapa));
        when(parcelaRepository.save(any())).thenAnswer(inv -> {
            Parcela p = inv.getArgument(0);
            setField(p, "id", 10L);
            return p;
        });
        when(manzanaRepository.findByParcelaIdOrderByNombreAsc(10L)).thenReturn(List.of());
        when(manzanaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ParcelaResponse result = parcelaService.createParcela(1L, req);

        assertNotNull(result);
        assertEquals("Nueva", result.getNombre());
        // 2 manzanas deben haberse creado
        verify(manzanaRepository, times(2)).save(any(Manzana.class));
    }

    @Test
    void createParcela_etapaNotFound_throws() {
        when(etapaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> parcelaService.createParcela(99L, new ParcelaRequest("X", 1, "Y")));
    }

    @Test
    void createParcela_manzanasAlreadySufficient_noExtraSaves() {
        ParcelaRequest req = new ParcelaRequest("Exist", 1, "Owner");

        when(etapaRepository.findById(1L)).thenReturn(Optional.of(etapa));
        when(parcelaRepository.save(any())).thenAnswer(inv -> {
            Parcela p = inv.getArgument(0);
            setField(p, "id", 11L);
            return p;
        });
        // Ya existe 1 manzana, requiredCount = 1 → no debe crear más
        Manzana existente = new Manzana();
        setField(existente, "id", 5L);
        existente.setNombre("Manzana A");
        existente.setParcela(parcela);
        when(manzanaRepository.findByParcelaIdOrderByNombreAsc(11L)).thenReturn(List.of(existente));

        parcelaService.createParcela(1L, req);

        verify(manzanaRepository, never()).save(any());
    }

    @Test
    void updateParcela_valid_updatesFields() {
        ParcelaRequest req = new ParcelaRequest("Actualizada", 1, "NewOwner");

        when(parcelaRepository.findById(1L)).thenReturn(Optional.of(parcela));
        when(manzanaRepository.findByParcelaIdOrderByNombreAsc(1L)).thenReturn(List.of());
        when(manzanaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(parcelaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ParcelaResponse result = parcelaService.updateParcela(1L, req);

        assertEquals("Actualizada", result.getNombre());
        assertEquals("NewOwner", result.getPropietario());
    }

    @Test
    void updateParcela_notFound_throws() {
        when(parcelaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> parcelaService.updateParcela(99L, new ParcelaRequest("X", 1, "Y")));
    }

    @Test
    void deleteParcela_existing_deletesAll() {
        when(parcelaRepository.existsById(1L)).thenReturn(true);

        parcelaService.deleteParcela(1L);

        verify(loteRepository).deleteAllByParcelaId(1L);
        verify(manzanaRepository).deleteAllByParcelaId(1L);
        verify(parcelaRepository).deleteById(1L);
    }

    @Test
    void deleteParcela_notFound_throws() {
        when(parcelaRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> parcelaService.deleteParcela(99L));
        verify(parcelaRepository, never()).deleteById(any());
    }

    // ════════════════════════════════════════════════════════════
    // ProformaService tests
    // ════════════════════════════════════════════════════════════

    @Test
    void create_withNullCodigo_generatesCode() {
        ProformaRequest req = buildProformaRequest(null);
        when(proformaRepository.existsByCodigo(any())).thenReturn(false);
        when(proformaRepository.save(any())).thenAnswer(inv -> {
            com.sisarovi.inmobiliario.entity.Proforma p = inv.getArgument(0);
            setField(p, "id", 1L);
            return p;
        });

        ProformaResponse result = proformaService.create(req, "admin");

        assertNotNull(result.getCodigo());
    }

    @Test
    void create_withValidCodigo_usesFormatted() {
        ProformaRequest req = buildProformaRequest("123ABC"); // formato correcto
        when(proformaRepository.existsByCodigo("123-ABC")).thenReturn(false);
        when(proformaRepository.save(any())).thenAnswer(inv -> {
            com.sisarovi.inmobiliario.entity.Proforma p = inv.getArgument(0);
            setField(p, "id", 2L);
            return p;
        });

        ProformaResponse result = proformaService.create(req, "admin");

        assertEquals("123-ABC", result.getCodigo());
    }

    @Test
    void create_codigoTaken_generatesAlternative() {
        ProformaRequest req = buildProformaRequest("123ABC");
        when(proformaRepository.existsByCodigo("123-ABC")).thenReturn(true); // ya existe
        when(proformaRepository.existsByCodigo(argThat(c -> !c.equals("123-ABC")))).thenReturn(false);
        when(proformaRepository.save(any())).thenAnswer(inv -> {
            com.sisarovi.inmobiliario.entity.Proforma p = inv.getArgument(0);
            setField(p, "id", 3L);
            return p;
        });

        ProformaResponse result = proformaService.create(req, "admin");
        assertNotNull(result.getCodigo());
    }

    @Test
    void create_withNullFechaEmision_savesNull() {
        ProformaRequest req = buildProformaRequest(null);
        req.setFechaEmision(null);
        when(proformaRepository.existsByCodigo(any())).thenReturn(false);
        when(proformaRepository.save(any())).thenAnswer(inv -> {
            com.sisarovi.inmobiliario.entity.Proforma p = inv.getArgument(0);
            setField(p, "id", 4L);
            return p;
        });

        ProformaResponse result = proformaService.create(req, "admin");
        assertNull(result.getFechaEmision());
    }

    @Test
    void create_stringExceedsMax_getsTrimmed() {
        ProformaRequest req = buildProformaRequest(null);
        req.setClienteNombre("A".repeat(100)); // > 40 chars
        when(proformaRepository.existsByCodigo(any())).thenReturn(false);
        when(proformaRepository.save(any())).thenAnswer(inv -> {
            com.sisarovi.inmobiliario.entity.Proforma p = inv.getArgument(0);
            setField(p, "id", 5L);
            return p;
        });

        proformaService.create(req, "admin"); // no debe lanzar excepción
        verify(proformaRepository).save(argThat(p ->
                p.getClienteNombre() != null && p.getClienteNombre().length() <= 40));
    }

    @Test
    void getById_existing_returnsProforma() {
        com.sisarovi.inmobiliario.entity.Proforma p = new com.sisarovi.inmobiliario.entity.Proforma();
        setField(p, "id", 1L);
        when(proformaRepository.findById(1L)).thenReturn(Optional.of(p));

        com.sisarovi.inmobiliario.entity.Proforma result = proformaService.getById(1L);
        assertNotNull(result);
    }

    @Test
    void getById_notFound_throws() {
        when(proformaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> proformaService.getById(99L));
    }

    @Test
    void buscar_shortQuery_returnsEmpty() {
        List<ProformaResponse> result = proformaService.buscar("nombre", "ab"); // < 3 chars
        assertTrue(result.isEmpty());
    }

    @Test
    void buscar_byCodigo_callsCodigoRepo() {
        when(proformaRepository.buscarTop50PorCodigoNormalizado("123ABC")).thenReturn(List.of());
        proformaService.buscar("codigo", "123-ABC");
        verify(proformaRepository).buscarTop50PorCodigoNormalizado("123ABC");
    }

    @Test
    void buscar_byNombre_callsNombreRepo() {
        when(proformaRepository.findTop50ByClienteNombreContainingIgnoreCaseOrderByCreatedAtDesc("Juan Garcia"))
                .thenReturn(List.of());
        proformaService.buscar("nombre", "Juan Garcia");
        verify(proformaRepository).findTop50ByClienteNombreContainingIgnoreCaseOrderByCreatedAtDesc("Juan Garcia");
    }

    @Test
    void buscar_nullQuery_returnsEmpty() {
        List<ProformaResponse> result = proformaService.buscar("nombre", null);
        assertTrue(result.isEmpty());
    }

    @Test
    void historial_delegatesToRepo() {
        when(proformaRepository.findHistorialLite()).thenReturn(List.of());
        proformaService.historial();
        verify(proformaRepository).findHistorialLite();
    }

    // ─── helpers ─────────────────────────────────────────────────────────

    private ProformaRequest buildProformaRequest(String codigo) {
        ProformaRequest req = new ProformaRequest();
        req.setCodigo(codigo);
        req.setProyecto("Proyecto Test");
        req.setClienteNombre("Juan Perez");
        req.setClienteDni("12345678");
        req.setClienteCelular("999888777");
        req.setAsesor("Asesor Test");
        req.setFechaEmision("2026-01-15");
        req.setFechaVencimiento("2026-02-15");
        req.setPrecioContado(new BigDecimal("150000"));
        return req;
    }

    private void setField(Object obj, String name, Object value) {
        try {
            var f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception ignored) {}
    }
}
