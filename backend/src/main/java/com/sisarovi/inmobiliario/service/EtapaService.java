package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.dto.EtapaRequest;
import com.sisarovi.inmobiliario.dto.EtapaResponse;
import com.sisarovi.inmobiliario.entity.Etapa;
import com.sisarovi.inmobiliario.entity.Project;
import com.sisarovi.inmobiliario.repository.EtapaRepository;
import com.sisarovi.inmobiliario.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EtapaService {
    
    private final EtapaRepository etapaRepository;
    private final ProjectRepository projectRepository;
    
    @Transactional(readOnly = true)
    public List<EtapaResponse> getEtapasByProject(Long projectId) {
        log.info("📊 Obteniendo etapas del proyecto ID: {}", projectId);
        
        List<Etapa> etapas = etapaRepository.findByProjectIdOrderByNumeroEtapaAsc(projectId);
        log.info("🔍 Etapas encontradas en BD: {}", etapas.size());
        
        if (etapas.isEmpty()) {
            log.warn("⚠️ No se encontraron etapas para el proyecto {}", projectId);
        }
        
        return etapas.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public EtapaResponse getEtapaById(Long etapaId) {
        log.info("Obteniendo etapa ID: {}", etapaId);
        Etapa etapa = etapaRepository.findById(etapaId)
                .orElseThrow(() -> new RuntimeException("Etapa no encontrada con ID: " + etapaId));
        return toResponse(etapa);
    }
    
    @Transactional
    public EtapaResponse createEtapa(Long projectId, EtapaRequest request) {
        log.info("Creando etapa {} para proyecto ID: {}", request.getNumeroEtapa(), projectId);
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado con ID: " + projectId));
        
        if (etapaRepository.existsByProjectIdAndNumeroEtapa(projectId, request.getNumeroEtapa())) {
            throw new RuntimeException("El número de Etapa ya existe");
        }
        
        Etapa etapa = new Etapa();
        etapa.setNumeroEtapa(request.getNumeroEtapa());
        etapa.setProject(project);
        
        Etapa saved = etapaRepository.save(etapa);
        log.info("Etapa creada con ID: {}", saved.getId());
        
        // Actualizar contador de etapas del proyecto
        project.setCantidadEtapas(etapaRepository.countByProjectId(projectId).intValue());
        projectRepository.save(project);
        log.info("✅ Contador de etapas actualizado: {}", project.getCantidadEtapas());
        
        return toResponse(saved);
    }
    
    @Transactional
    public EtapaResponse updateEtapa(Long etapaId, EtapaRequest request) {
        log.info("Actualizando etapa ID: {}", etapaId);
        
        Etapa etapa = etapaRepository.findById(etapaId)
                .orElseThrow(() -> new RuntimeException("Etapa no encontrada con ID: " + etapaId));
        
        if (!etapa.getNumeroEtapa().equals(request.getNumeroEtapa()) &&
            etapaRepository.existsByProjectIdAndNumeroEtapa(etapa.getProject().getId(), request.getNumeroEtapa())) {
            throw new RuntimeException("El número de Etapa ya existe");
        }
        
        etapa.setNumeroEtapa(request.getNumeroEtapa());
        
        Etapa updated = etapaRepository.save(etapa);
        log.info("Etapa actualizada: {}", updated.getId());
        
        return toResponse(updated);
    }
    
    @Transactional
    public void deleteEtapa(Long etapaId) {
        log.info("Eliminando etapa ID: {}", etapaId);
        
        Etapa etapa = etapaRepository.findById(etapaId)
                .orElseThrow(() -> new RuntimeException("Etapa no encontrada con ID: " + etapaId));
        
        Long projectId = etapa.getProject().getId();
        Project project = etapa.getProject();
        
        etapaRepository.deleteById(etapaId);
        log.info("Etapa eliminada: {}", etapaId);
        
        // Actualizar contador de etapas del proyecto
        project.setCantidadEtapas(etapaRepository.countByProjectId(projectId).intValue());
        projectRepository.save(project);
        log.info("✅ Contador de etapas actualizado: {}", project.getCantidadEtapas());
    }
    
    private EtapaResponse toResponse(Etapa etapa) {
        return EtapaResponse.builder()
                .id(etapa.getId())
                .numeroEtapa(etapa.getNumeroEtapa())
                .cantidadParcelas(etapa.getCantidadParcelas())
                .projectId(etapa.getProject().getId())
                .build();
    }
}
