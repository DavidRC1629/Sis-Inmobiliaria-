package com.sisarovi.inmobiliario;

import com.sisarovi.inmobiliario.dto.ProjectRequest;
import com.sisarovi.inmobiliario.dto.ProjectResponse;
import com.sisarovi.inmobiliario.entity.Role;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.entity.UserStatus;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.repository.ProjectRepository;
import com.sisarovi.inmobiliario.repository.RoleRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import com.sisarovi.inmobiliario.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectServiceIntegrationTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ClienteRepository clienteRepository;

    private User adminUser;

    @BeforeEach
    void setupAdmin() {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));
        if (userRepository.findByDni("admin").isEmpty()) {
            adminUser = User.builder()
                    .dni("admin")
                    .email("admin@local.test")
                    .nombres("Admin")
                    .primerApellido("Coverage")
                    .segundoApellido("Test")
                    .password(passwordEncoder.encode("admin123"))
                    .role(adminRole)
                    .estado(UserStatus.ACTIVO)
                    .enabled(true)
                    .build();
            adminUser = userRepository.save(adminUser);
        } else {
            adminUser = userRepository.findByDni("admin").orElseThrow();
        }
    }

    @Test
    void testCreateUpdateAndDeleteProjectFlow() {
        String projectName = "Coverage Project " + UUID.randomUUID().toString().substring(0, 8);

        ProjectRequest request = new ProjectRequest();
        request.setNombre(projectName);
        request.setImagenUrl("http://example.com/image.png");
        request.setLogoUrl("http://example.com/logo.png");
        request.setCantidadEtapas(2);

        ProjectResponse created = projectService.createProject(request, adminUser.getDni());
        assertNotNull(created);
        assertEquals(projectName.trim(), created.getNombre());
        assertEquals(2, created.getCantidadEtapas());

        ProjectResponse loaded = projectService.getProjectById(created.getId());
        assertEquals(created.getId(), loaded.getId());
        assertEquals(created.getNombre(), loaded.getNombre());

        request.setNombre(projectName + " Updated");
        request.setCantidadEtapas(3);
        request.setLogoUrl("http://example.com/logo-updated.png");

        ProjectResponse updated = projectService.updateProject(created.getId(), request);
        assertEquals(request.getNombre().trim(), updated.getNombre());
        assertEquals(3, updated.getCantidadEtapas());

        projectService.updateProjectLogo(created.getId(), "http://example.com/new-logo.png");
        ProjectResponse withNewLogo = projectService.getProjectById(created.getId());
        assertEquals("http://example.com/new-logo.png", withNewLogo.getLogoUrl());

        projectService.deleteProject(created.getId());
        assertFalse(projectRepository.existsById(created.getId()));
    }
}
