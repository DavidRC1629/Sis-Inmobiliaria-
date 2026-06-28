package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.ClienteResponse;
import com.sisarovi.inmobiliario.entity.Cliente;
import com.sisarovi.inmobiliario.entity.Etapa;
import com.sisarovi.inmobiliario.entity.Lote;
import com.sisarovi.inmobiliario.entity.Manzana;
import com.sisarovi.inmobiliario.entity.Parcela;
import com.sisarovi.inmobiliario.entity.Project;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.repository.LoteRepository;
import com.sisarovi.inmobiliario.repository.ProjectRepository;
import com.sisarovi.inmobiliario.service.CronogramaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private LoteRepository loteRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CronogramaService cronogramaService;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente;
    private Lote lote;

    @BeforeEach
    void setUp() {
        Project project = new Project();
        project.setId(100L);
        project.setNombre("Proyecto Prueba");
        project.setCantidadEtapas(1);

        Etapa etapa = new Etapa();
        etapa.setId(200L);
        etapa.setNumeroEtapa(1);
        etapa.setProject(project);

        Parcela parcela = new Parcela();
        parcela.setId(20L);
        parcela.setNombre("Parcela A");
        parcela.setNumManzanas(1);
        parcela.setNumLotes(1);
        parcela.setPropietario("Propietario Prueba");
        parcela.setLotesDisponibles(1);
        parcela.setEtapa(etapa);

        Manzana manzana = new Manzana();
        manzana.setId(30L);
        manzana.setNombre("Manzana 1");
        manzana.setParcela(parcela);

        lote = new Lote();
        lote.setId(10L);
        lote.setNumero(1);
        lote.setParcela(parcela);
        lote.setManzana(manzana);

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombres("Juan");
        cliente.setApellidos("Pérez");
        cliente.setDni("12345678");
        cliente.setEmail("juan.perez@example.com");
        cliente.setTelefono("555-1234");
        cliente.setDireccion("Calle Falsa 123");
        cliente.setClienteDesde(LocalDate.of(2024, 1, 1));
        cliente.setTipoRelacion("ADQUISICION");
        cliente.setLote(lote);
    }

    @Test
    @DisplayName("getClienteById debe devolver respuesta cuando el cliente existe")
    void testGetClienteByIdSuccess() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        ClienteResponse response = clienteService.getClienteById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Juan", response.getNombres());
        assertEquals("Pérez", response.getApellidos());
        assertEquals("12345678", response.getDni());
        assertEquals("juan.perez@example.com", response.getEmail());
        assertEquals("555-1234", response.getTelefono());
        assertEquals("Calle Falsa 123", response.getDireccion());
        assertEquals("ADQUISICION", response.getTipoRelacion());
        assertEquals(LocalDate.of(2024, 1, 1), response.getClienteDesde());
        assertEquals(10L, response.getLoteId());
        assertEquals(1, response.getLoteNumero());
        assertEquals("Manzana 1", response.getManzana());
        assertEquals("Parcela A", response.getParcelaNombre());
        assertEquals(1, response.getEtapaNumero());
        assertEquals(100L, response.getProjectId());
        assertEquals("Proyecto Prueba", response.getProjectNombre());

        verify(clienteRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getClienteById debe lanzar excepción cuando el cliente no existe")
    void testGetClienteByIdNotFound() {
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> clienteService.getClienteById(999L));

        assertEquals("Cliente no encontrado", exception.getMessage());
        verify(clienteRepository, times(1)).findById(999L);
    }

    @Test
    void getAllClientes() {
    }

    @Test
    void getProjectSummaries() {
    }

    @Test
    void getClientesByProject() {
    }

    @Test
    void getLotesDisponibles() {
    }

    @Test
    void createClientesPorAdquisicion() {
    }

    @Test
    void createCliente() {
    }

    @Test
    void updateCliente() {
    }

    @Test
    void deleteCliente() {
    }

    @Test
    void getClienteById() {
    }
}
