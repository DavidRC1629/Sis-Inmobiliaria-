package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.dto.ProjectRequest;
import com.sisarovi.inmobiliario.dto.ProjectResponse;
import com.sisarovi.inmobiliario.entity.Etapa;
import com.sisarovi.inmobiliario.entity.Project;
import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.repository.ProjectRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import com.sisarovi.inmobiliario.service.ProjectService;
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
class ProjectServiceUnitTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClienteRepository clienteRepository;

    @InjectMocks private ProjectService projectService;

    private User adminUser;
    private Project project;

    @BeforeEach
    void setUp() {
        Role adminRole = Role.builder().name("ROLE_ADMIN").build();
        setField(adminRole, "id", 1L);

        adminUser = User.builder()
                .dni("admin")
                .nombres("Admin")
                .primerApellido("Test")
                .segundoApellido("")
                .password("pw")
                .email("admin@test.com")
                .role(adminRole)
                .estado(UserStatus.ACTIVO)
                .enabled(true)
                .build();
        setField(adminUser, "id", 1L);

        project = new Project();
        setField(project, "id", 10L);
        project.setNombre("Proyecto Test");
        project.setCantidadEtapas(2);
        project.setCreatedBy(adminUser);
        project.setEtapas(new ArrayList<>());
    }

    // ─── getAllProjects ──────────────────────────────────────────────────────

    @Test
    void getAllProjects_returnsMappedList() {
        when(projectRepository.findAll()).thenReturn(List.of(project));

        List<ProjectResponse> result = projectService.getAllProjects();

        assertEquals(1, result.size());
        assertEquals("Proyecto Test", result.get(0).getNombre());
    }

    @Test
    void getAllProjects_empty_returnsEmptyList() {
        when(projectRepository.findAll()).thenReturn(List.of());
        assertTrue(projectService.getAllProjects().isEmpty());
    }

    // ─── getProjectById ──────────────────────────────────────────────────────

    @Test
    void getProjectById_existing_returnsResponse() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        ProjectResponse result = projectService.getProjectById(10L);

        assertEquals("Proyecto Test", result.getNombre());
    }

    @Test
    void getProjectById_notFound_throws() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> projectService.getProjectById(99L));
        assertTrue(ex.getMessage().contains("99"));
    }

    // ─── createProject ───────────────────────────────────────────────────────

    @Test
    void createProject_valid_savesProject() {
        ProjectRequest req = buildRequest("Nuevo Proyecto", 3);
        when(projectRepository.existsByNombreNormalized("Nuevo Proyecto")).thenReturn(false);
        when(userRepository.findByDni("admin")).thenReturn(Optional.of(adminUser));
        when(projectRepository.save(any())).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            setField(p, "id", 20L);
            return p;
        });

        ProjectResponse result = projectService.createProject(req, "admin");

        assertNotNull(result);
        assertEquals("Nuevo Proyecto", result.getNombre());
        assertEquals(3, result.getCantidadEtapas());
    }

    @Test
    void createProject_duplicateName_throws() {
        ProjectRequest req = buildRequest("Existente", 1);
        when(projectRepository.existsByNombreNormalized("Existente")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> projectService.createProject(req, "admin"));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void createProject_userNotFound_throws() {
        ProjectRequest req = buildRequest("Nuevo", 1);
        when(projectRepository.existsByNombreNormalized("Nuevo")).thenReturn(false);
        when(userRepository.findByDni("ghost")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> projectService.createProject(req, "ghost"));
    }

    @Test
    void createProject_userDisabled_throws() {
        adminUser.setEnabled(false);
        ProjectRequest req = buildRequest("Nuevo", 1);
        when(projectRepository.existsByNombreNormalized("Nuevo")).thenReturn(false);
        when(userRepository.findByDni("admin")).thenReturn(Optional.of(adminUser));

        assertThrows(RuntimeException.class, () -> projectService.createProject(req, "admin"));
    }

    @Test
    void createProject_userNotAdmin_throws() {
        Role userRole = Role.builder().name("ROLE_USER").build();
        setField(userRole, "id", 2L);
        adminUser.setRole(userRole);

        ProjectRequest req = buildRequest("Nuevo", 1);
        when(projectRepository.existsByNombreNormalized("Nuevo")).thenReturn(false);
        when(userRepository.findByDni("admin")).thenReturn(Optional.of(adminUser));

        assertThrows(RuntimeException.class, () -> projectService.createProject(req, "admin"));
    }

    @Test
    void createProject_withNullLogoUrl_savesWithoutLogo() {
        ProjectRequest req = buildRequest("SinLogo", 1);
        req.setLogoUrl(null);
        when(projectRepository.existsByNombreNormalized("SinLogo")).thenReturn(false);
        when(userRepository.findByDni("admin")).thenReturn(Optional.of(adminUser));
        when(projectRepository.save(any())).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            setField(p, "id", 21L);
            return p;
        });

        ProjectResponse result = projectService.createProject(req, "admin");
        assertNotNull(result);
    }

    // ─── updateProject ───────────────────────────────────────────────────────

    @Test
    void updateProject_sameCantidad_noEtapaChange() {
        project.setCantidadEtapas(2);
        ProjectRequest req = buildRequest("Updated", 2); // mismo count
        when(projectRepository.existsByNombreNormalizedAndIdNot("Updated", 10L)).thenReturn(false);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponse result = projectService.updateProject(10L, req);

        assertEquals("Updated", result.getNombre());
        assertEquals(2, result.getCantidadEtapas());
    }

    @Test
    void updateProject_moreCantidad_addsEtapas() {
        project.setCantidadEtapas(1);
        ProjectRequest req = buildRequest("Updated", 3); // aumenta de 1 a 3
        when(projectRepository.existsByNombreNormalizedAndIdNot("Updated", 10L)).thenReturn(false);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponse result = projectService.updateProject(10L, req);

        assertEquals(3, result.getCantidadEtapas());
    }

    @Test
    void updateProject_lessCantidad_removesEtapas() {
        // Agrega 3 etapas a project
        for (int i = 1; i <= 3; i++) {
            Etapa e = new Etapa();
            e.setNumeroEtapa(i);
            e.setProject(project);
            e.setParcelas(new ArrayList<>());
            project.getEtapas().add(e);
        }
        project.setCantidadEtapas(3);

        ProjectRequest req = buildRequest("Updated", 1); // baja de 3 a 1
        when(projectRepository.existsByNombreNormalizedAndIdNot("Updated", 10L)).thenReturn(false);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponse result = projectService.updateProject(10L, req);

        assertEquals(1, result.getCantidadEtapas());
    }

    @Test
    void updateProject_duplicateName_throws() {
        ProjectRequest req = buildRequest("Otro", 2);
        when(projectRepository.existsByNombreNormalizedAndIdNot("Otro", 10L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> projectService.updateProject(10L, req));
    }

    @Test
    void updateProject_notFound_throws() {
        ProjectRequest req = buildRequest("X", 1);
        when(projectRepository.existsByNombreNormalizedAndIdNot("X", 99L)).thenReturn(false);
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> projectService.updateProject(99L, req));
    }

    // ─── deleteProject ───────────────────────────────────────────────────────

    @Test
    void deleteProject_valid_deletesSuccessfully() {
        when(projectRepository.existsById(10L)).thenReturn(true);
        when(clienteRepository.existsByProjectId(10L)).thenReturn(false);

        projectService.deleteProject(10L);

        verify(projectRepository).deleteById(10L);
    }

    @Test
    void deleteProject_notFound_throws() {
        when(projectRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> projectService.deleteProject(99L));
        verify(projectRepository, never()).deleteById(any());
    }

    @Test
    void deleteProject_hasClientes_throws() {
        when(projectRepository.existsById(10L)).thenReturn(true);
        when(clienteRepository.existsByProjectId(10L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> projectService.deleteProject(10L));
        verify(projectRepository, never()).deleteById(any());
    }

    // ─── updateProjectLogo ───────────────────────────────────────────────────

    @Test
    void updateProjectLogo_valid_updatesLogo() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponse result = projectService.updateProjectLogo(10L, "http://new-logo.png");

        assertEquals("http://new-logo.png", result.getLogoUrl());
    }

    @Test
    void updateProjectLogo_notFound_throws() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> projectService.updateProjectLogo(99L, "url"));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private ProjectRequest buildRequest(String nombre, int etapas) {
        ProjectRequest req = new ProjectRequest();
        req.setNombre(nombre);
        req.setCantidadEtapas(etapas);
        req.setImagenUrl("http://img.png");
        req.setLogoUrl("http://logo.png");
        return req;
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            var f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception ignored) {}
    }
}
