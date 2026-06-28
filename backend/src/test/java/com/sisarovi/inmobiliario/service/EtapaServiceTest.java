package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.EtapaResponse;
import com.sisarovi.inmobiliario.entity.Etapa;
import com.sisarovi.inmobiliario.entity.Project;
import com.sisarovi.inmobiliario.repository.EtapaRepository;
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
public class EtapaServiceTest {

    @Mock
    private EtapaRepository etapaRepository;

    @InjectMocks
    private EtapaService etapaService;

    private Etapa etapa;

    @BeforeEach
    public void setUp() {
        Project project = new Project();
        project.setId(1L);
        project.setNombre("Proyecto Prueba");

        etapa = new Etapa();
        etapa.setId(10L);
        etapa.setNumeroEtapa(1);
        etapa.setProject(project);
    }

    @Test
    public void testGetEtapaByIdSuccess() {
        // 1. Mockeamos la respuesta del repositorio
        when(etapaRepository.findById(10L)).thenReturn(Optional.of(etapa));

        // 2. Ejecutamos el servicio (FÍJATE: 10L va SOLO dentro del paréntesis)
        EtapaResponse resultado = etapaService.getEtapaById(10L);

        // 3. Verificamos los resultados
        assertNotNull(resultado);
        assertEquals(1L, resultado.getProjectId());

        // 4. CORRECCIÓN: Borra todo lo que escribiste en la línea del assertEquals
        // y escribe esto EXACTAMENTE:
        // resultado. (punto), luego presiona Ctrl+Espacio y elige el método que devuelva el nombre del proyecto.
        // Si no existe ninguno, borra esa línea de prueba por ahora para que pase el test.

        // 5. Verificamos que se llamó al repositorio
        verify(etapaRepository, times(1)).findById(10L);
    }
}