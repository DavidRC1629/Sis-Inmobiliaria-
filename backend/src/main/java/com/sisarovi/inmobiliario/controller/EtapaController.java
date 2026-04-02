package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.dto.EtapaRequest;
import com.sisarovi.inmobiliario.dto.EtapaResponse;
import com.sisarovi.inmobiliario.service.EtapaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/projects/{projectId}/etapas")
@RequiredArgsConstructor
public class EtapaController {
    
    private final EtapaService etapaService;
    
    @GetMapping
    public ResponseEntity<List<EtapaResponse>> getEtapasByProject(@PathVariable Long projectId) {
        log.info("🔵 GET /api/projects/{}/etapas", projectId);
        List<EtapaResponse> etapas = etapaService.getEtapasByProject(projectId);
        log.info("✅ Devolviendo {} etapas para proyecto {}", etapas.size(), projectId);
        return ResponseEntity.ok(etapas);
    }
    
    @GetMapping("/{etapaId}")
    public ResponseEntity<EtapaResponse> getEtapaById(@PathVariable Long projectId, @PathVariable Long etapaId) {
        return ResponseEntity.ok(etapaService.getEtapaById(etapaId));
    }
    
    @PostMapping
    public ResponseEntity<EtapaResponse> createEtapa(
            @PathVariable Long projectId,
            @Valid @RequestBody EtapaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(etapaService.createEtapa(projectId, request));
    }
    
    @PutMapping("/{etapaId}")
    public ResponseEntity<EtapaResponse> updateEtapa(
            @PathVariable Long projectId,
            @PathVariable Long etapaId,
            @Valid @RequestBody EtapaRequest request) {
        return ResponseEntity.ok(etapaService.updateEtapa(etapaId, request));
    }
    
    @DeleteMapping("/{etapaId}")
    public ResponseEntity<Void> deleteEtapa(@PathVariable Long projectId, @PathVariable Long etapaId) {
        etapaService.deleteEtapa(etapaId);
        return ResponseEntity.noContent().build();
    }
}
