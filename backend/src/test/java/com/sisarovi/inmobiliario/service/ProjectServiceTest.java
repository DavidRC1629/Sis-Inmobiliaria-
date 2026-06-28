package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.ProjectRequest;
import com.sisarovi.inmobiliario.entity.Project;
import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.repository.ProjectRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock
    ProjectRepository projectRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ClienteRepository clienteRepository;

    @InjectMocks
    ProjectService projectService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setNombres("Admin");
        adminUser.setDni("99999999");
        adminUser.setEnabled(true);
        adminUser.setEstado(UserStatus.ACTIVO);
        Role role = new Role();
        role.setName("ROLE_ADMIN");
        adminUser.setRole(role);
    }

    @Test
    @DisplayName("createProject lanza excepción si usuario no existe")
    void createProjectUserNotFound() {
        when(projectRepository.existsByNombreNormalized(anyString())).thenReturn(false);
        when(userRepository.findByDni("x")).thenReturn(Optional.empty());

        ProjectRequest req = new ProjectRequest();
        req.setNombre(" Nuevo ");
        req.setCantidadEtapas(1);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> projectService.createProject(req, "x"));
        assertTrue(ex.getMessage().contains("Usuario autenticado no encontrado"));
    }

    @Test
    @DisplayName("createProject crea proyecto correctamente")
    void createProjectSuccess() {
        when(projectRepository.existsByNombreNormalized(anyString())).thenReturn(false);
        when(userRepository.findByDni("99999999")).thenReturn(Optional.of(adminUser));
        Project saved = new Project();
        saved.setId(50L);
        saved.setNombre("Nuevo");
        saved.setCreatedBy(adminUser);
        saved.setCantidadEtapas(2);
        when(projectRepository.save(any())).thenReturn(saved);

        ProjectRequest req = new ProjectRequest();
        req.setNombre(" Nuevo ");
        req.setCantidadEtapas(2);

        var resp = projectService.createProject(req, "99999999");
        assertNotNull(resp);
        assertEquals(50L, resp.getId());
        assertEquals(2, resp.getCantidadEtapas());
    }

    @Test
    @DisplayName("deleteProject lanza excepción si no existe")
    void deleteProjectNotFound() {
        when(projectRepository.existsById(77L)).thenReturn(false);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> projectService.deleteProject(77L));
        assertTrue(ex.getMessage().contains("Proyecto no encontrado"));
    }

    @Test
    @DisplayName("deleteProject lanza excepción si hay clientes asociados")
    void deleteProjectHasClientes() {
        when(projectRepository.existsById(2L)).thenReturn(true);
        when(clienteRepository.existsByProjectId(2L)).thenReturn(true);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> projectService.deleteProject(2L));
        assertTrue(ex.getMessage().contains("No se puede eliminar este proyecto"));
    }
}
