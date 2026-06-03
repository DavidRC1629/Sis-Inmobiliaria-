package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.ParcelaRequest;
import com.sisarovi.inmobiliario.dto.ParcelaResponse;
import com.sisarovi.inmobiliario.entity.*;
import com.sisarovi.inmobiliario.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParcelaServiceTest {

    @Mock private ParcelaRepository parcelaRepository;
    @Mock private EtapaRepository etapaRepository;
    @Mock private LoteRepository loteRepository;
    @Mock private ManzanaRepository manzanaRepository;

    @InjectMocks
    private ParcelaService parcelaService;

    private Parcela parcela;
    private Etapa etapa;

    @BeforeEach
    public void setUp() {
        Project project = new Project();
        project.setId(1L);

        etapa = new Etapa();
        etapa.setId(100L);
        etapa.setProject(project);

        parcela = new Parcela();
        parcela.setId(10L);
        parcela.setNombre("Parcela A");
        parcela.setNumManzanas(1);
        parcela.setPropietario("Propietario");
        parcela.setEtapa(etapa);
    }

    @Test
    public void testGetParcelaByIdSuccess() {
        when(parcelaRepository.findById(10L)).thenReturn(Optional.of(parcela));

        ParcelaResponse resultado = parcelaService.getParcelaById(10L);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals("Parcela A", resultado.getNombre());
        verify(parcelaRepository, times(1)).findById(10L);
    }

    @Test
    public void testCreateParcelaSuccess() {
        when(etapaRepository.findById(100L)).thenReturn(Optional.of(etapa));
        when(parcelaRepository.save(any(Parcela.class))).thenReturn(parcela);

        ParcelaRequest request = new ParcelaRequest();
        request.setNombre("Parcela A");
        request.setNumManzanas(1);
        request.setPropietario("Propietario");

        ParcelaResponse resultado = parcelaService.createParcela(100L, request);

        assertNotNull(resultado);
        verify(parcelaRepository, times(1)).save(any(Parcela.class));
        verify(manzanaRepository, times(1)).save(any(Manzana.class));
    }
}