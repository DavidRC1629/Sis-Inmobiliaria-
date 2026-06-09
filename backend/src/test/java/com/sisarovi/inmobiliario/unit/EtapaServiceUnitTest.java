package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.dto.EtapaRequest;
import com.sisarovi.inmobiliario.dto.EtapaResponse;
import com.sisarovi.inmobiliario.entity.Etapa;
import com.sisarovi.inmobiliario.entity.Project;
import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.repository.EtapaRepository;
import com.sisarovi.inmobiliario.repository.ProjectRepository;
import com.sisarovi.inmobiliario.service.EtapaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EtapaServiceUnitTest {

    @Mock private EtapaRepository etapaRepository;
    @Mock private ProjectRepository projectRepository;

    @InjectMocks
    private EtapaService etapaService;

    private Project project;
    private Etapa etapa;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().name("ROLE_ADMIN").build();
        User user = User.builder()
                .dni("admin")
                .nombres("Admin")
                .primerApellido("Test")
                .segundoApellido("")
                .password("pw")
                .email("admin@test.com")
                .role(role)
                .estado(UserStatus.ACTIVO)
                .enabled(true)
                .build();
        setField(user, "id", 1L);

        project = new Project();
        setField(project, "id", 1L);
        project.setNombre("Proyecto Alpha");
        project.setCantidadEtapas(1);
        project.setEtapas(new ArrayList<>());
        project.setCreatedBy(user);

        etapa = new Etapa();
        setField(etapa, "id", 10L);
        etapa.setNumeroEtapa(1);
        etapa.setProject(project);
        etapa.setParcelas(new ArrayList<>());
    }

    // ─── getEtapasByProject ─────────────────────────────────────────────────────

    @Test
    void getEtapasByProject_returnsListOfResponses() {
        when(etapaRepository.findByProjectIdOrderByNumeroEtapaAsc(1L)).thenReturn(List.of(etapa));

        List<EtapaResponse> result = etapaService.getEtapasByProject(1L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getNumeroEtapa());
        assertEquals(1L, result.get(0).getProjectId());
    }

    @Test
    void getEtapasByProject_noEtapas_returnsEmpty() {
        when(etapaRepository.findByProjectIdOrderByNumeroEtapaAsc(99L)).thenReturn(List.of());

        List<EtapaResponse> result = etapaService.getEtapasByProject(99L);

        assertTrue(result.isEmpty());
    }

    // ─── getEtapaById ───────────────────────────────────────────────────────────

    @Test
    void getEtapaById_existingId_returnsResponse() {
        when(etapaRepository.findById(10L)).thenReturn(Optional.of(etapa));

        EtapaResponse result = etapaService.getEtapaById(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(1, result.getNumeroEtapa());
    }

    @Test
    void getEtapaById_notFound_throwsException() {
        when(etapaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> etapaService.getEtapaById(99L));
    }

    // ─── createEtapa ────────────────────────────────────────────────────────────

    @Test
    void createEtapa_validRequest_savesAndUpdatesCounter() {
        EtapaRequest request = new EtapaRequest();
        request.setNumeroEtapa(2);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(etapaRepository.existsByProjectIdAndNumeroEtapa(1L, 2)).thenReturn(false);
        when(etapaRepository.save(any(Etapa.class))).thenAnswer(inv -> {
            Etapa e = inv.getArgument(0);
            setField(e, "id", 20L);
            return e;
        });
        when(etapaRepository.countByProjectId(1L)).thenReturn(2L);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        EtapaResponse result = etapaService.createEtapa(1L, request);

        assertNotNull(result);
        assertEquals(2, result.getNumeroEtapa());
        verify(etapaRepository).save(any(Etapa.class));
        verify(projectRepository).save(project);
    }

    @Test
    void createEtapa_duplicateNumero_throwsException() {
        EtapaRequest request = new EtapaRequest();
        request.setNumeroEtapa(1);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(etapaRepository.existsByProjectIdAndNumeroEtapa(1L, 1)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> etapaService.createEtapa(1L, request));
        verify(etapaRepository, never()).save(any());
    }

    @Test
    void createEtapa_projectNotFound_throwsException() {
        EtapaRequest request = new EtapaRequest();
        request.setNumeroEtapa(1);

        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> etapaService.createEtapa(99L, request));
    }

    // ─── updateEtapa ────────────────────────────────────────────────────────────

    @Test
    void updateEtapa_validRequest_updatesNumero() {
        EtapaRequest request = new EtapaRequest();
        request.setNumeroEtapa(5);

        when(etapaRepository.findById(10L)).thenReturn(Optional.of(etapa));
        when(etapaRepository.existsByProjectIdAndNumeroEtapa(1L, 5)).thenReturn(false);
        when(etapaRepository.save(any(Etapa.class))).thenAnswer(inv -> inv.getArgument(0));

        EtapaResponse result = etapaService.updateEtapa(10L, request);

        assertEquals(5, result.getNumeroEtapa());
    }

    @Test
    void updateEtapa_sameNumero_doesNotCheckDuplicate() {
        EtapaRequest request = new EtapaRequest();
        request.setNumeroEtapa(1); // same as existing

        when(etapaRepository.findById(10L)).thenReturn(Optional.of(etapa));
        when(etapaRepository.save(any(Etapa.class))).thenAnswer(inv -> inv.getArgument(0));

        // Should not throw even without mock for existsByProjectIdAndNumeroEtapa
        EtapaResponse result = etapaService.updateEtapa(10L, request);
        assertEquals(1, result.getNumeroEtapa());
    }

    @Test
    void updateEtapa_notFound_throwsException() {
        EtapaRequest request = new EtapaRequest();
        request.setNumeroEtapa(2);

        when(etapaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> etapaService.updateEtapa(99L, request));
    }

    // ─── deleteEtapa ────────────────────────────────────────────────────────────

    @Test
    void deleteEtapa_existingId_deletesAndUpdatesCounter() {
        when(etapaRepository.findById(10L)).thenReturn(Optional.of(etapa));
        when(etapaRepository.countByProjectId(1L)).thenReturn(0L);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        etapaService.deleteEtapa(10L);

        verify(etapaRepository).deleteById(10L);
        verify(projectRepository).save(project);
    }

    @Test
    void deleteEtapa_notFound_throwsException() {
        when(etapaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> etapaService.deleteEtapa(99L));
        verify(etapaRepository, never()).deleteById(any());
    }

    // ─── helper ─────────────────────────────────────────────────────────────────

    private void setField(Object obj, String fieldName, Object value) {
        try {
            var f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            // try superclass
            try {
                var f = obj.getClass().getSuperclass().getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(obj, value);
            } catch (Exception ignored) {}
        }
    }
}
