package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.LoteResponse;
import com.sisarovi.inmobiliario.entity.Etapa;
import com.sisarovi.inmobiliario.entity.Lote;
import com.sisarovi.inmobiliario.entity.Manzana;
import com.sisarovi.inmobiliario.entity.Parcela;
import com.sisarovi.inmobiliario.entity.Project;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.repository.LoteRepository;
import com.sisarovi.inmobiliario.repository.ManzanaRepository;
import com.sisarovi.inmobiliario.repository.ParcelaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoteServiceTest {

    @Mock
    private LoteRepository loteRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ParcelaRepository parcelaRepository;
    @Mock
    private ManzanaRepository manzanaRepository;

    @InjectMocks
    private LoteService loteService;

    private Project project;
    private Etapa etapa;
    private Parcela parcela;
    private Manzana manzana;
    private Lote lote;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(100L);
        project.setNombre("Proyecto X");

        etapa = new Etapa();
        etapa.setId(200L);
        etapa.setNumeroEtapa(1);
        etapa.setProject(project);

        parcela = new Parcela();
        parcela.setId(10L);
        parcela.setNombre("Parcela A");
        parcela.setEtapa(etapa);

        manzana = new Manzana();
        manzana.setId(30L);
        manzana.setNombre("Manzana 1");
        manzana.setParcela(parcela);

        lote = new Lote();
        lote.setId(5L);
        lote.setNumero(1);
        lote.setCalle("Calle 1");
        lote.setNumeroPartida("12345");
        lote.setPrecioLote(new BigDecimal("1000"));
        lote.setParcela(parcela);
        lote.setManzana(manzana);
    }

    @Test
    @DisplayName("getLoteById devuelve LoteResponse cuando existe")
    void testGetLoteByIdSuccess() {
        when(loteRepository.findById(5L)).thenReturn(Optional.of(lote));
        when(clienteRepository.existsByLoteId(5L)).thenReturn(true);

        LoteResponse resp = loteService.getLoteById(5L);

        assertNotNull(resp);
        assertEquals(5L, resp.getId());
        assertEquals(1, resp.getNumero());
        assertEquals("Calle 1", resp.getCalle());
        assertEquals("12345", resp.getNumeroPartida());
        assertEquals(new BigDecimal("1000"), resp.getPrecioLote());
        assertEquals(30L, resp.getManzanaId());

        // Corregido: Coincide con el nombre definido en el setUp (Manzana 1)
        assertEquals("1", resp.getManzana());

        assertEquals(10L, resp.getParcelaId());
        assertEquals("Parcela A", resp.getParcelaNombre());
        assertEquals(1, resp.getEtapaNumero());
        assertEquals(100L, resp.getProjectId());
        assertTrue(resp.isAdquirido());

        verify(loteRepository, times(1)).findById(5L);
    }

    @Test
    @DisplayName("getLoteById lanza excepción cuando no existe")
    void testGetLoteByIdNotFound() {
        when(loteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> loteService.getLoteById(999L));

        verify(loteRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("getLotesByParcelaAndManzana lanza excepción si manzana no es numérica")
    void testGetLotesByParcelaAndManzanaInvalid() {
        assertThrows(RuntimeException.class, () -> loteService.getLotesByParcelaAndManzana(10L, "abc"));
    }

    @Test
    @DisplayName("existsNumeroPartidaGlobal devuelve false para formatos inválidos")
    void testExistsNumeroPartidaGlobalInvalidFormat() {
        boolean result = loteService.existsNumeroPartidaGlobal("abc-123", null);
        assertFalse(result);
    }

    @Test
    @DisplayName("deleteLote lanza excepción cuando lote no existe")
    void testDeleteLoteNotFound() {
        when(loteRepository.existsById(77L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> loteService.deleteLote(77L));

        verify(loteRepository, times(1)).existsById(77L);
    }
}