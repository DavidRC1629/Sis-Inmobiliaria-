package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.dto.ProjectRequest;
import com.sisarovi.inmobiliario.dto.ProjectResponse;
import com.sisarovi.inmobiliario.service.ProjectService;
import com.sisarovi.inmobiliario.service.RegistroAuditoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {
    
    private final ProjectService projectService;
    private final RegistroAuditoriaService registroAuditoriaService;
    
        @GetMapping
        public ResponseEntity<List<ProjectResponse>> getAllProjects() {
            return ResponseEntity.ok(projectService.getAllProjects());
        }
    
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getProjectById(projectId));
    }
    
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping
        public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {
        log.info("🔵 POST /api/projects - Usuario: {}, Authorities: {}", 
                authentication.getName(), authentication.getAuthorities());
        log.info("📦 Request: nombre={}, cantidadEtapas={}", request.getNombre(), request.getCantidadEtapas());
        
        String currentUserDni = authentication.getName();
        ProjectResponse created = projectService.createProject(request, currentUserDni);
        registroAuditoriaService.registrarAccion(
            currentUserDni,
            "CREATE",
            String.format("Se creó el proyecto '%s'.", created.getNombre())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
        @PreAuthorize("hasRole('ADMIN')")
        @PutMapping("/{projectId}")
        public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {
        log.info("🟠 PUT /api/projects/{} - Datos recibidos:", projectId);
        log.info("📦 nombre={}, cantidadEtapas={}, imagenUrl={}", 
                request.getNombre(), request.getCantidadEtapas(), 
                request.getImagenUrl() != null ? "presente" : "null");

        ProjectResponse beforeUpdate = projectService.getProjectById(projectId);
        
        ProjectResponse response = projectService.updateProject(projectId, request);
        String descripcionCambios = construirDescripcionCambiosProyecto(beforeUpdate, response, request);

        registroAuditoriaService.registrarAccion(
            authentication != null ? authentication.getName() : "ANONIMO",
            "UPDATE",
            descripcionCambios
        );
        
        log.info("✅ Proyecto actualizado - ID: {}, Etapas: {}", 
                response.getId(), response.getCantidadEtapas());
        
        return ResponseEntity.ok(response);
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId, Authentication authentication) {
        ProjectResponse beforeDelete = projectService.getProjectById(projectId);
        projectService.deleteProject(projectId);
        registroAuditoriaService.registrarAccion(
                authentication != null ? authentication.getName() : "ANONIMO",
                "DELETE",
            String.format("Se eliminó el proyecto '%s'.", beforeDelete.getNombre())
        );
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{projectId}/logo")
    public ResponseEntity<ProjectResponse> updateProjectLogo(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        String logoUrl = request != null ? request.getOrDefault("logoUrl", "") : "";
        ProjectResponse updated = projectService.updateProjectLogo(projectId, logoUrl);
        String accion = logoUrl == null || logoUrl.trim().isEmpty() ? "eliminó" : "actualizó";
        registroAuditoriaService.registrarAccion(
                authentication != null ? authentication.getName() : "ANONIMO",
                "UPDATE",
            String.format("Se %s el logo del proyecto '%s'.", accion, updated.getNombre())
        );
        return ResponseEntity.ok(updated);
    }

    private String construirDescripcionCambiosProyecto(ProjectResponse beforeUpdate, ProjectResponse afterUpdate, ProjectRequest request) {
        List<String> cambios = new ArrayList<>();

        String nombreAnterior = limpiarTexto(beforeUpdate.getNombre());
        String nombreNuevo = limpiarTexto(request.getNombre());
        if (!Objects.equals(nombreAnterior, nombreNuevo)) {
            cambios.add(String.format("Se cambió el nombre del proyecto de '%s' a '%s'", nombreAnterior, nombreNuevo));
        }

        Integer etapasAnteriores = beforeUpdate.getCantidadEtapas();
        Integer etapasNuevas = request.getCantidadEtapas();
        if (!Objects.equals(etapasAnteriores, etapasNuevas)) {
            cambios.add(String.format("se cambió la cantidad de etapas de %d a %d", etapasAnteriores, etapasNuevas));
        }

        String imagenAnterior = limpiarTexto(beforeUpdate.getImagenUrl());
        String imagenNueva = limpiarTexto(request.getImagenUrl());
        if (!Objects.equals(imagenAnterior, imagenNueva)) {
            cambios.add("se actualizó la imagen del proyecto");
        }

        String logoAnterior = limpiarTexto(beforeUpdate.getLogoUrl());
        String logoNuevo = limpiarTexto(request.getLogoUrl());
        if (!Objects.equals(logoAnterior, logoNuevo)) {
            cambios.add("se actualizó el logo del proyecto");
        }

        String nombreFinal = limpiarTexto(afterUpdate.getNombre());
        if (cambios.isEmpty()) {
            return String.format("Se actualizó el proyecto '%s' sin cambios visibles.", nombreFinal);
        }

        return String.format("Se actualizó el proyecto '%s': %s.",
                nombreFinal,
                String.join(", ", cambios));
    }

    private String limpiarTexto(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.trim();
    }
}
