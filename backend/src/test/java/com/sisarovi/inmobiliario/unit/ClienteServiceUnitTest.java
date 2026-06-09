package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.dto.ClienteRequest;
import com.sisarovi.inmobiliario.dto.ClienteResponse;
import com.sisarovi.inmobiliario.entity.*;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.repository.LoteRepository;
import com.sisarovi.inmobiliario.repository.ProjectRepository;
import com.sisarovi.inmobiliario.service.ClienteService;
import com.sisarovi.inmobiliario.service.CronogramaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

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
class ClienteServiceUnitTest {

    @Mock ClienteRepository clienteRepository;
    @Mock LoteRepository loteRepository;
    @Mock ProjectRepository projectRepository;
    @Mock CronogramaService cronogramaService;

    @InjectMocks ClienteService clienteService;

    private Lote lote;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().name("ROLE_ADMIN").build();
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
                .email("j@t.com").telefono("999888777").direccion("Av. Test")
                .tipoRelacion("ADQUISICION").clienteDesde(LocalDate.now()).lote(lote)
                .build();
        setField(cliente, "id", 1L);
    }

    // ─── getAllClientes ───────────────────────────────────────────────────

    @Test
    void getAllClientes_noQuery_returnAll() {
        when(clienteRepository.findAllOrdered()).thenReturn(List.of(cliente));

        List<ClienteResponse> result = clienteService.getAllClientes(null);

        assertEquals(1, result.size());
        assertEquals("Juan", result.get(0).getNombres());
    }

    @Test
    void getAllClientes_queryMatchesName_returnsFiltered() {
        when(clienteRepository.findAllOrdered()).thenReturn(List.of(cliente));

        List<ClienteResponse> result = clienteService.getAllClientes("juan");
        assertEquals(1, result.size());
    }

    @Test
    void getAllClientes_queryNoMatch_returnsEmpty() {
        when(clienteRepository.findAllOrdered()).thenReturn(List.of(cliente));

        List<ClienteResponse> result = clienteService.getAllClientes("zzzzz");
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllClientes_queryMatchesDni_returnsFiltered() {
        when(clienteRepository.findAllOrdered()).thenReturn(List.of(cliente));

        List<ClienteResponse> result = clienteService.getAllClientes("12345678");
        assertEquals(1, result.size());
    }

    // ─── getProjectSummaries ─────────────────────────────────────────────

    @Test
    void getProjectSummaries_returnsSorted() {
        Project p = new Project();
        setField(p, "id", 1L);
        p.setNombre("Proyecto Alpha");

        Role role = Role.builder().name("ROLE_ADMIN").build();
        setField(role, "id", 1L);
        User user = User.builder().dni("a").nombres("A").primerApellido("B").segundoApellido("")
                .password("pw").email("a@b.com").role(role).estado(UserStatus.ACTIVO).enabled(true).build();
        setField(user, "id", 1L);
        p.setCreatedBy(user);
        p.setCantidadEtapas(1);
        p.setEtapas(new ArrayList<>());

        when(projectRepository.findAll()).thenReturn(List.of(p));
        when(clienteRepository.findByProjectIdOrdered(1L)).thenReturn(List.of(cliente));

        var result = clienteService.getProjectSummaries();

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getCantidadClientes());
    }

    // ─── getClientesByProject ────────────────────────────────────────────

    @Test
    void getClientesByProject_returnsList() {
        when(clienteRepository.findByProjectIdOrdered(1L)).thenReturn(List.of(cliente));

        List<ClienteResponse> result = clienteService.getClientesByProject(1L);

        assertEquals(1, result.size());
    }

    // ─── getClienteById ──────────────────────────────────────────────────

    @Test
    void getClienteById_existing_returns() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        ClienteResponse result = clienteService.getClienteById(1L);
        assertEquals("Juan", result.getNombres());
    }

    @Test
    void getClienteById_notFound_throws() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> clienteService.getClienteById(99L));
    }

    // ─── createCliente ───────────────────────────────────────────────────

    @Test
    void createCliente_valid_saves() {
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(clienteRepository.existsByLoteId(1L)).thenReturn(false);
        when(clienteRepository.save(any())).thenReturn(cliente);

        ClienteRequest req = new ClienteRequest("Juan", "Perez", "12345678", "j@t.com",
                "999", "Av.", 1L, "ADQUISICION", LocalDate.now());

        ClienteResponse result = clienteService.createCliente(req);
        assertNotNull(result);
        verify(clienteRepository).save(any());
    }

    @Test
    void createCliente_loteNotFound_throws() {
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());

        ClienteRequest req = new ClienteRequest("Juan", "Perez", "12345678", null,
                "999", "Av.", 99L, "ADQUISICION", LocalDate.now());

        assertThrows(RuntimeException.class, () -> clienteService.createCliente(req));
    }

    @Test
    void createCliente_loteAdquirido_throws() {
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(clienteRepository.existsByLoteId(1L)).thenReturn(true);

        ClienteRequest req = new ClienteRequest("Juan", "Perez", "12345678", null,
                "999", "Av.", 1L, "ADQUISICION", LocalDate.now());

        assertThrows(RuntimeException.class, () -> clienteService.createCliente(req));
    }

    @Test
    void createCliente_tipoRelacionInvalido_throws() {
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(clienteRepository.existsByLoteId(1L)).thenReturn(false);

        ClienteRequest req = new ClienteRequest("Juan", "Perez", "12345678", null,
                "999", "Av.", 1L, "INVALIDO", LocalDate.now());

        assertThrows(RuntimeException.class, () -> clienteService.createCliente(req));
    }

    // ─── updateCliente ───────────────────────────────────────────────────

    @Test
    void updateCliente_sameLote_updates() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(clienteRepository.save(any())).thenReturn(cliente);

        ClienteRequest req = new ClienteRequest("Carlos", "Lopez", "87654321", null,
                "888", "Jr.", 1L, "SEPARACION", LocalDate.now());

        ClienteResponse result = clienteService.updateCliente(1L, req);
        assertNotNull(result);
        verify(clienteRepository).save(any());
    }

    @Test
    void updateCliente_differentLote_validatesDisponible() {
        Lote otroLote = new Lote();
        setField(otroLote, "id", 2L);
        otroLote.setNumero(2);
        otroLote.setNumeroPartida("87654321");
        otroLote.setPrecioLote(new BigDecimal("60000"));
        otroLote.setParcela(lote.getParcela());
        otroLote.setManzana(lote.getManzana());

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(loteRepository.findById(2L)).thenReturn(Optional.of(otroLote));
        when(clienteRepository.existsByLoteId(2L)).thenReturn(false);
        when(clienteRepository.save(any())).thenReturn(cliente);

        ClienteRequest req = new ClienteRequest("Carlos", "Lopez", "87654321", null,
                "888", "Jr.", 2L, "ADQUISICION", LocalDate.now());

        clienteService.updateCliente(1L, req);
        verify(clienteRepository).save(any());
    }

    @Test
    void updateCliente_notFound_throws() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        ClienteRequest req = new ClienteRequest("X", "Y", "12345678", null,
                "999", "Av.", 1L, "ADQUISICION", LocalDate.now());

        assertThrows(RuntimeException.class, () -> clienteService.updateCliente(99L, req));
    }

    // ─── deleteCliente ───────────────────────────────────────────────────

    @Test
    void deleteCliente_existing_deletes() {
        when(clienteRepository.existsById(1L)).thenReturn(true);

        clienteService.deleteCliente(1L);

        verify(clienteRepository).deleteById(1L);
    }

    @Test
    void deleteCliente_notFound_throws() {
        when(clienteRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> clienteService.deleteCliente(99L));
    }

    // ─── getLotesDisponibles ─────────────────────────────────────────────

    @Test
    void getLotesDisponibles_returnsList() {
        when(loteRepository.findByProjectIdForClientes(1L)).thenReturn(List.of(lote));

        var result = clienteService.getLotesDisponibles(1L, null);
        assertEquals(1, result.size());
    }

    // ─── helper ──────────────────────────────────────────────────────────

    private void setField(Object obj, String name, Object value) {
        try {
            var f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception ignored) {}
    }
}
