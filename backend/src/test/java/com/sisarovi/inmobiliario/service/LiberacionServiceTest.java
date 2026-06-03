package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.LiberacionLoteResponse;
import com.sisarovi.inmobiliario.entity.Cliente;
import com.sisarovi.inmobiliario.entity.CronogramaContrato;
import com.sisarovi.inmobiliario.entity.Lote;
import com.sisarovi.inmobiliario.entity.Manzana;
import com.sisarovi.inmobiliario.entity.Parcela;
import com.sisarovi.inmobiliario.entity.Etapa;
import com.sisarovi.inmobiliario.entity.Project;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.repository.CronogramaContratoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiberacionServiceTest {
    @Mock
    ClienteRepository clienteRepository;
    @Mock
    CronogramaContratoRepository cronogramaContratoRepository;
    @Mock
    AdminService adminService;
    @Mock
    RegistroAuditoriaService registroAuditoriaService;

    @InjectMocks
    LiberacionService liberacionService;

    private Cliente c1;
    private Cliente c2;
    private Lote lote;

    @BeforeEach
    void setUp() {
        Project project = new Project();
        project.setId(1L);
        project.setNombre("P1");

        Etapa etapa = new Etapa();
        etapa.setId(2L);
        etapa.setNumeroEtapa(1);
        etapa.setProject(project);

        Parcela parcela = new Parcela();
        parcela.setId(3L);
        parcela.setNombre("Parcela X");
        parcela.setEtapa(etapa);

        Manzana manzana = new Manzana();
        manzana.setId(4L);
        manzana.setNombre("A");

        lote = new Lote();
        lote.setId(10L);
        lote.setNumero(5);
        lote.setParcela(parcela);
        lote.setManzana(manzana);

        c1 = new Cliente();
        c1.setId(100L);
        c1.setNombres("Juan");
        c1.setApellidos("Perez");
        c1.setDni("11111111");
        c1.setLote(lote);

        c2 = new Cliente();
        c2.setId(101L);
        c2.setNombres("Ana");
        c2.setApellidos("Lopez");
        c2.setDni("22222222");
        c2.setLote(lote);
    }

    @Test
    @DisplayName("listarLotesAdquiridosPorProyecto devuelve vacío si no hay clientes")
    void listarEmpty() {
        when(clienteRepository.findByProjectIdOrdered(1L)).thenReturn(List.of());
        List<LiberacionLoteResponse> res = liberacionService.listarLotesAdquiridosPorProyecto(1L);
        assertNotNull(res);
        assertTrue(res.isEmpty());
    }

    @Test
    @DisplayName("listarLotesAdquiridosPorProyecto devuelve lista con datos cuando hay titulares")
    void listarWithData() {
        when(clienteRepository.findByProjectIdOrdered(1L)).thenReturn(List.of(c1, c2));
        CronogramaContrato contrato = new CronogramaContrato();
        contrato.setId(500L);
        contrato.setCliente(c1);
        contrato.setMontoPagadoTotal(new BigDecimal("0"));
        when(cronogramaContratoRepository.findDetailedByLoteIds(List.of(10L))).thenReturn(List.of(contrato));

        List<LiberacionLoteResponse> res = liberacionService.listarLotesAdquiridosPorProyecto(1L);
        assertNotNull(res);
        assertEquals(1, res.size());
        LiberacionLoteResponse r = res.get(0);
        assertEquals(10L, r.getLoteId());
        assertEquals(5, r.getLoteNumero());
        assertEquals("A", r.getManzana());
        assertEquals("Parcela X", r.getParcelaNombre());
        assertFalse(r.isMoroso());
        assertFalse(r.isRequierePasswordAdmin());
    }

    @Test
    @DisplayName("liberarLote lanza excepción si no hay titulares")
    void liberarNoTitulares() {
        when(clienteRepository.findAllByLoteId(999L)).thenReturn(List.of());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> liberacionService.liberarLote(999L, "razon", "pw", "user"));
        assertTrue(ex.getMessage().contains("El lote ya no tiene titulares"));
    }

    @Test
    @DisplayName("liberarLote borra contratos y registra auditoria cuando hay monto pagado")
    void liberarWithPago() {
        when(clienteRepository.findAllByLoteId(10L)).thenReturn(List.of(c1, c2));
        CronogramaContrato contrato = new CronogramaContrato();
        contrato.setMontoPagadoTotal(new BigDecimal("100"));
        contrato.setCliente(c1);
        when(cronogramaContratoRepository.findByClienteIdIn(List.of(100L,101L))).thenReturn(List.of(contrato));

        doNothing().when(adminService).validarPasswordAdmin("user","pw");
        doNothing().when(cronogramaContratoRepository).deleteByClienteIdIn(List.of(100L,101L));
        doNothing().when(clienteRepository).deleteAll(List.of(c1,c2));
        doNothing().when(registroAuditoriaService).registrarAccion(
                anyString(), anyString(), anyString(), anyString(), anyString(), any(BigDecimal.class), anyString(), any(), any(), any(), any(), any(), any());

        liberacionService.liberarLote(10L, "devolucion", "pw", "user");

        verify(adminService, times(1)).validarPasswordAdmin("user","pw");
        verify(cronogramaContratoRepository, times(1)).deleteByClienteIdIn(List.of(100L,101L));
        verify(clienteRepository, times(1)).deleteAll(List.of(c1,c2));
        verify(registroAuditoriaService, times(1)).registrarAccion(
                anyString(), anyString(), anyString(), anyString(), anyString(), any(BigDecimal.class), anyString(), any(), any(), any(), any(), any(), any());
    }
}
