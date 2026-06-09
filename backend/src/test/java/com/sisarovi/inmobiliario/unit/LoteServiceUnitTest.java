package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.dto.LoteRequest;
import com.sisarovi.inmobiliario.dto.LoteResponse;
import com.sisarovi.inmobiliario.entity.*;
import com.sisarovi.inmobiliario.repository.*;
import com.sisarovi.inmobiliario.service.LoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class LoteServiceUnitTest {

    @Mock private LoteRepository loteRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ParcelaRepository parcelaRepository;
    @Mock private ManzanaRepository manzanaRepository;

    @InjectMocks private LoteService loteService;

    private Parcela parcela;
    private Manzana manzana;
    private Lote lote;

    @BeforeEach
    void setUp() {
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

        Etapa etapa = new Etapa();
        setField(etapa, "id", 1L);
        etapa.setNumeroEtapa(1);
        etapa.setProject(project);
        etapa.setParcelas(new ArrayList<>());

        parcela = new Parcela();
        setField(parcela, "id", 1L);
        parcela.setNombre("Parcela A");
        parcela.setNumManzanas(1);
        parcela.setNumLotes(0);
        parcela.setPropietario("Prop");
        parcela.setLotesDisponibles(0);
        parcela.setEtapa(etapa);
        parcela.setLotes(new ArrayList<>());

        manzana = new Manzana();
        setField(manzana, "id", 1L);
        manzana.setNombre("Manzana A");
        manzana.setParcela(parcela);

        lote = new Lote();
        setField(lote, "id", 10L);
        lote.setNumero(1);
        lote.setNumeroPartida("12345678");
        lote.setPrecioLote(new BigDecimal("50000"));
        lote.setParcela(parcela);
        lote.setManzana(manzana);
    }

    // ─── getLotesByParcela ────────────────────────────────────────────────

    @Test
    void getLotesByParcela_returnsList() {
        when(loteRepository.findByParcelaIdOrderByNumeroAsc(1L)).thenReturn(List.of(lote));
        when(clienteRepository.existsByLoteId(10L)).thenReturn(false);

        List<LoteResponse> result = loteService.getLotesByParcela(1L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getNumero());
    }

    @Test
    void getLotesByParcela_empty_returnsEmpty() {
        when(loteRepository.findByParcelaIdOrderByNumeroAsc(99L)).thenReturn(List.of());
        assertTrue(loteService.getLotesByParcela(99L).isEmpty());
    }

    // ─── getLotesByParcelaAndManzana ──────────────────────────────────────

    @Test
    void getLotesByParcelaAndManzana_validId_returnsList() {
        when(loteRepository.findByParcelaIdAndManzanaIdOrderByNumeroAsc(1L, 1L)).thenReturn(List.of(lote));
        when(clienteRepository.existsByLoteId(10L)).thenReturn(false);

        List<LoteResponse> result = loteService.getLotesByParcelaAndManzana(1L, "1");
        assertEquals(1, result.size());
    }

    @Test
    void getLotesByParcelaAndManzana_invalidId_throws() {
        assertThrows(RuntimeException.class,
                () -> loteService.getLotesByParcelaAndManzana(1L, "abc"));
    }

    // ─── getLoteById ─────────────────────────────────────────────────────

    @Test
    void getLoteById_existing_returnsResponse() {
        when(loteRepository.findById(10L)).thenReturn(Optional.of(lote));
        when(clienteRepository.existsByLoteId(10L)).thenReturn(false);

        LoteResponse result = loteService.getLoteById(10L);
        assertEquals(1, result.getNumero());
    }

    @Test
    void getLoteById_notFound_throws() {
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> loteService.getLoteById(99L));
    }

    // ─── createLote ──────────────────────────────────────────────────────

    @Test
    void createLote_valid_savesLote() {
        LoteRequest req = buildRequest(2, "87654321", new BigDecimal("60000"));
        req.setManzanaId(1L);

        when(parcelaRepository.findById(1L)).thenReturn(Optional.of(parcela));
        when(manzanaRepository.findById(1L)).thenReturn(Optional.of(manzana));
        when(loteRepository.findByParcelaIdAndManzana_IdAndNumero(1L, 1L, 2)).thenReturn(Optional.empty());
        when(loteRepository.findByNumeroPartidaIgnoreCase("87654321")).thenReturn(Optional.empty());
        when(loteRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Lote l = inv.getArgument(0);
            setField(l, "id", 20L);
            return l;
        });
        when(clienteRepository.existsByLoteId(20L)).thenReturn(false);

        LoteResponse result = loteService.createLote(1L, req);

        assertNotNull(result);
        assertEquals(2, result.getNumero());
    }

    @Test
    void createLote_parcelaNotFound_throws() {
        when(parcelaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> loteService.createLote(99L, buildRequest(1, "11111111", new BigDecimal("1000"))));
    }

    @Test
    void createLote_manzanaNull_throws() {
        LoteRequest req = buildRequest(1, "11111111", new BigDecimal("1000"));
        req.setManzanaId(null);
        req.setManzana(null); // no manzana

        when(parcelaRepository.findById(1L)).thenReturn(Optional.of(parcela));
        // findByParcelaIdOrderByIdAsc no será llamado porque manzana == null detiene el flujo
        lenient().when(manzanaRepository.findByParcelaIdOrderByIdAsc(1L)).thenReturn(List.of());

        assertThrows(RuntimeException.class, () -> loteService.createLote(1L, req));
    }

    @Test
    void createLote_duplicateLoteNumeroEnManzana_throws() {
        LoteRequest req = buildRequest(1, "11111111", new BigDecimal("1000"));
        req.setManzanaId(1L);

        when(parcelaRepository.findById(1L)).thenReturn(Optional.of(parcela));
        when(manzanaRepository.findById(1L)).thenReturn(Optional.of(manzana));
        when(loteRepository.findByParcelaIdAndManzana_IdAndNumero(1L, 1L, 1)).thenReturn(Optional.of(lote));

        assertThrows(RuntimeException.class, () -> loteService.createLote(1L, req));
    }

    @Test
    void createLote_duplicatePartida_throws() {
        LoteRequest req = buildRequest(2, "12345678", new BigDecimal("1000")); // ya existe
        req.setManzanaId(1L);

        when(parcelaRepository.findById(1L)).thenReturn(Optional.of(parcela));
        when(manzanaRepository.findById(1L)).thenReturn(Optional.of(manzana));
        when(loteRepository.findByParcelaIdAndManzana_IdAndNumero(1L, 1L, 2)).thenReturn(Optional.empty());
        when(loteRepository.findByNumeroPartidaIgnoreCase("12345678")).thenReturn(Optional.of(lote));

        assertThrows(RuntimeException.class, () -> loteService.createLote(1L, req));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ABCD1234", "123456789"})
    void createLote_invalidPartida_throws(String partida) {
        LoteRequest req = buildRequest(2, partida, new BigDecimal("1000"));
        req.setManzanaId(1L);
        when(parcelaRepository.findById(1L)).thenReturn(Optional.of(parcela));
        when(manzanaRepository.findById(1L)).thenReturn(Optional.of(manzana));
        when(loteRepository.findByParcelaIdAndManzana_IdAndNumero(1L, 1L, 2)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> loteService.createLote(1L, req));
    }

    @Test
    void createLote_zeroPrecio_throws() {
        LoteRequest req = buildRequest(2, "11111111", BigDecimal.ZERO);
        req.setManzanaId(1L);
        when(parcelaRepository.findById(1L)).thenReturn(Optional.of(parcela));
        when(manzanaRepository.findById(1L)).thenReturn(Optional.of(manzana));
        when(loteRepository.findByParcelaIdAndManzana_IdAndNumero(1L, 1L, 2)).thenReturn(Optional.empty());
        when(loteRepository.findByNumeroPartidaIgnoreCase("11111111")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> loteService.createLote(1L, req));
    }

    // ─── updateLote ──────────────────────────────────────────────────────

    @Test
    void updateLote_sameNumber_samePartida_updatesSuccessfully() {
        LoteRequest req = buildRequest(1, "12345678", new BigDecimal("70000")); // mismos valores
        req.setManzanaId(1L);

        when(loteRepository.findById(10L)).thenReturn(Optional.of(lote));
        when(manzanaRepository.findById(1L)).thenReturn(Optional.of(manzana));
        when(loteRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clienteRepository.existsByLoteId(10L)).thenReturn(false);

        LoteResponse result = loteService.updateLote(10L, req);
        assertNotNull(result);
        assertEquals(new BigDecimal("70000"), result.getPrecioLote());
    }

    @Test
    void updateLote_notFound_throws() {
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> loteService.updateLote(99L, buildRequest(1, "12345678", new BigDecimal("1000"))));
    }

    @Test
    void updateLote_newPartidaAlreadyExists_throws() {
        LoteRequest req = buildRequest(1, "99999999", new BigDecimal("50000")); // nueva partida
        req.setManzanaId(1L);

        Lote otherLote = new Lote();
        setField(otherLote, "id", 99L);
        otherLote.setNumeroPartida("99999999");

        when(loteRepository.findById(10L)).thenReturn(Optional.of(lote));
        when(manzanaRepository.findById(1L)).thenReturn(Optional.of(manzana));
        when(loteRepository.findByNumeroPartidaIgnoreCase("99999999")).thenReturn(Optional.of(otherLote));

        assertThrows(RuntimeException.class, () -> loteService.updateLote(10L, req));
    }

    // ─── deleteLote ──────────────────────────────────────────────────────

    @Test
    void deleteLote_existing_deletes() {
        when(loteRepository.existsById(10L)).thenReturn(true);

        loteService.deleteLote(10L);

        verify(loteRepository).deleteById(10L);
    }

    @Test
    void deleteLote_notFound_throws() {
        when(loteRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> loteService.deleteLote(99L));
        verify(loteRepository, never()).deleteById(any());
    }

    // ─── existsNumeroPartidaGlobal ────────────────────────────────────────

    @Test
    void existsNumeroPartidaGlobal_validFound_returnsTrue() {
        when(loteRepository.findByNumeroPartidaIgnoreCase("12345678")).thenReturn(Optional.of(lote));
        assertTrue(loteService.existsNumeroPartidaGlobal("12345678", null));
    }

    @Test
    void existsNumeroPartidaGlobal_sameExcludeId_returnsFalse() {
        when(loteRepository.findByNumeroPartidaIgnoreCase("12345678")).thenReturn(Optional.of(lote));
        assertFalse(loteService.existsNumeroPartidaGlobal("12345678", 10L));
    }

    @Test
    void existsNumeroPartidaGlobal_notFound_returnsFalse() {
        when(loteRepository.findByNumeroPartidaIgnoreCase("11111111")).thenReturn(Optional.empty());
        assertFalse(loteService.existsNumeroPartidaGlobal("11111111", null));
    }

    @Test
    void existsNumeroPartidaGlobal_empty_returnsFalse() {
        assertFalse(loteService.existsNumeroPartidaGlobal("", null));
    }

    @Test
    void existsNumeroPartidaGlobal_lettersInPartida_returnsFalse() {
        assertFalse(loteService.existsNumeroPartidaGlobal("ABCD1234", null));
    }

    // ─── resolveManzana — by name ─────────────────────────────────────────

    @Test
    void createLote_resolveManzanaByName_finds() {
        LoteRequest req = buildRequest(5, "55555555", new BigDecimal("10000"));
        req.setManzanaId(null);
        req.setManzana("Manzana A");

        when(parcelaRepository.findById(1L)).thenReturn(Optional.of(parcela));
        when(manzanaRepository.findByParcelaIdOrderByIdAsc(1L)).thenReturn(List.of(manzana));
        when(loteRepository.findByParcelaIdAndManzana_IdAndNumero(1L, 1L, 5)).thenReturn(Optional.empty());
        when(loteRepository.findByNumeroPartidaIgnoreCase("55555555")).thenReturn(Optional.empty());
        when(loteRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Lote l = inv.getArgument(0);
            setField(l, "id", 50L);
            return l;
        });
        when(clienteRepository.existsByLoteId(50L)).thenReturn(false);

        LoteResponse result = loteService.createLote(1L, req);
        assertNotNull(result);
    }

    // ─── helpers ─────────────────────────────────────────────────────────

    private LoteRequest buildRequest(int numero, String partida, BigDecimal precio) {
        LoteRequest req = new LoteRequest();
        req.setNumero(numero);
        req.setNumeroPartida(partida);
        req.setPrecioLote(precio);
        req.setCalle("Av. Test");
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
