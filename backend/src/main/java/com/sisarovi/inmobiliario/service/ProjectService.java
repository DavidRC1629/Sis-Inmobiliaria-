package com.sisarovi.inmobiliario.service;

import java.util.ArrayList;

import com.sisarovi.inmobiliario.dto.ProjectRequest;
import com.sisarovi.inmobiliario.dto.ProjectResponse;
import com.sisarovi.inmobiliario.repository.ClienteRepository;
import com.sisarovi.inmobiliario.entity.Etapa;
import com.sisarovi.inmobiliario.entity.Project;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.repository.ProjectRepository;
import com.sisarovi.inmobiliario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {
    
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ClienteRepository clienteRepository;
    
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        log.info("Obteniendo todos los proyectos");
        return projectRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId) {
        log.info("Obteniendo proyecto ID: {}", projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado con ID: " + projectId));
        return toResponse(project);
    }
    
    @Transactional
    public ProjectResponse createProject(ProjectRequest request, String currentUserDni) {
        log.info("Creando proyecto: {} por usuario: {} con {} etapas", 
            request.getNombre(), currentUserDni, request.getCantidadEtapas());

        String normalizedNombre = request.getNombre() != null ? request.getNombre().trim() : "";
        if (projectRepository.existsByNombreNormalized(normalizedNombre)) {
            throw new RuntimeException("Ya existe un proyecto con ese nombre");
        }

        User currentUser = userRepository.findByDni(currentUserDni)
            .orElse(null);
        if (currentUser == null) {
            log.error("Usuario no encontrado con DNI: {}", currentUserDni);
            throw new RuntimeException("Usuario autenticado no encontrado en la base de datos");
        }
        if (!currentUser.isEnabled() || !"ACTIVO".equalsIgnoreCase(String.valueOf(currentUser.getEstado()))) {
            log.error("Usuario {} no está activo o habilitado. Estado: {}, Enabled: {}", currentUserDni, currentUser.getEstado(), currentUser.isEnabled());
            throw new RuntimeException("Usuario autenticado no está activo o habilitado");
        }
        if (!"ROLE_ADMIN".equalsIgnoreCase(currentUser.getRole().getName())) {
            log.error("Usuario {} no tiene permisos de ADMIN. Rol actual: {}", currentUserDni, currentUser.getRole().getName());
            throw new RuntimeException("Solo los administradores pueden crear proyectos");
        }

        Project project = new Project();
        project.setNombre(normalizedNombre);
        project.setImagenUrl(request.getImagenUrl());
        if (request.getLogoUrl() != null) {
            project.setLogoUrl(request.getLogoUrl());
        }
        project.setCreatedBy(currentUser);
        project.setCantidadEtapas(request.getCantidadEtapas());

        // Crear etapas automáticamente (sin parcelas)
        for (int i = 1; i <= request.getCantidadEtapas(); i++) {
            Etapa etapa = Etapa.builder()
                .numeroEtapa(i)
                .project(project)
                .parcelas(new ArrayList<>()) // No crear parcelas aquí
                .build();
            project.getEtapas().add(etapa);
        }

        Project saved = projectRepository.save(project);
        log.info("Proyecto creado con ID: {} y {} etapas", saved.getId(), saved.getEtapas().size());

        return toResponse(saved);
    }
    
    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {
        log.info("Actualizando proyecto ID: {} con {} etapas", projectId, request.getCantidadEtapas());

        String normalizedNombre = request.getNombre() != null ? request.getNombre().trim() : "";
        if (projectRepository.existsByNombreNormalizedAndIdNot(normalizedNombre, projectId)) {
            throw new RuntimeException("Ya existe un proyecto con ese nombre");
        }
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado con ID: " + projectId));
        
        project.setNombre(normalizedNombre);
        project.setImagenUrl(request.getImagenUrl());
        project.setLogoUrl(request.getLogoUrl());
        
        // Actualizar cantidad de etapas
        int cantidadAnterior = project.getCantidadEtapas();
        int cantidadNueva = request.getCantidadEtapas();
        
        if (cantidadNueva != cantidadAnterior) {
            log.info("Cambiando cantidad de etapas de {} a {}", cantidadAnterior, cantidadNueva);
            project.setCantidadEtapas(cantidadNueva);
            
            // Si hay más etapas, agregarlas
            if (cantidadNueva > cantidadAnterior) {
                for (int i = cantidadAnterior + 1; i <= cantidadNueva; i++) {
                    Etapa etapa = Etapa.builder()
                            .numeroEtapa(i)
                            .project(project)
                            .build();
                    project.getEtapas().add(etapa);
                }
                log.info("Se agregaron {} etapas nuevas", cantidadNueva - cantidadAnterior);
            }
            // Si hay menos etapas, eliminar las sobrantes
            else if (cantidadNueva < cantidadAnterior) {
                project.getEtapas().removeIf(etapa -> etapa.getNumeroEtapa() > cantidadNueva);
                log.info("Se eliminaron {} etapas", cantidadAnterior - cantidadNueva);
            }
        }
        
        Project updated = projectRepository.save(project);
        log.info("Proyecto actualizado: {} con {} etapas", updated.getId(), updated.getEtapas().size());
        
        return toResponse(updated);
    }
    @Transactional
    public void deleteProject(Long projectId) {
        log.info("Eliminando proyecto ID: {}", projectId);
        
        if (!projectRepository.existsById(projectId)) {
            throw new RuntimeException("Proyecto no encontrado con ID: " + projectId);
        }

        if (clienteRepository.existsByProjectId(projectId)) {
            throw new RuntimeException("No se puede eliminar este proyecto porque tiene lotes ya adquiridos o separados por clientes");
        }
        
        projectRepository.deleteById(projectId);
        log.info("Proyecto eliminado: {}", projectId);
    }

    @Transactional
    public ProjectResponse updateProjectLogo(Long projectId, String logoUrl) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado con ID: " + projectId));

        project.setLogoUrl(logoUrl);
        Project updated = projectRepository.save(project);
        return toResponse(updated);
    }
    
    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
            .id(project.getId())
            .nombre(project.getNombre())
            .imagenUrl(project.getImagenUrl())
            .logoUrl(project.getLogoUrl())
            .createdByNombre(project.getCreatedBy().getNombres())
            .cantidadEtapas(project.getCantidadEtapas())
            .cantidadParcelasTotal(project.getCantidadParcelasTotal())
            .build();
    }
}
