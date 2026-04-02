package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.dto.ParcelaRequest;
import com.sisarovi.inmobiliario.dto.ParcelaResponse;
import com.sisarovi.inmobiliario.service.ParcelaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ParcelaController {
    
    private final ParcelaService parcelaService;
    
    // Endpoint directo para obtener parcela por ID (sin necesitar etapaId)
    @GetMapping("/api/parcelas/{parcelaId}")
    public ResponseEntity<ParcelaResponse> getParcelaDirectById(@PathVariable Long parcelaId) {
        return ResponseEntity.ok(parcelaService.getParcelaById(parcelaId));
    }
    
    @GetMapping("/api/etapas/{etapaId}/parcelas")
    public ResponseEntity<List<ParcelaResponse>> getParcelasByEtapa(@PathVariable Long etapaId) {
        return ResponseEntity.ok(parcelaService.getParcelasByEtapa(etapaId));
    }
    
    @GetMapping("/api/etapas/{etapaId}/parcelas/{parcelaId}")
    public ResponseEntity<ParcelaResponse> getParcelaById(@PathVariable Long etapaId, @PathVariable Long parcelaId) {
        return ResponseEntity.ok(parcelaService.getParcelaById(parcelaId));
    }
    
    @GetMapping("/api/etapas/{etapaId}/parcelas/search")
    public ResponseEntity<List<ParcelaResponse>> searchByPropietario(
            @PathVariable Long etapaId,
            @RequestParam String propietario) {
        return ResponseEntity.ok(parcelaService.searchByPropietario(propietario));
    }
    
    @PostMapping("/api/etapas/{etapaId}/parcelas")
    public ResponseEntity<ParcelaResponse> createParcela(
            @PathVariable Long etapaId,
            @Valid @RequestBody ParcelaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(parcelaService.createParcela(etapaId, request));
    }
    
    @PutMapping("/api/etapas/{etapaId}/parcelas/{parcelaId}")
    public ResponseEntity<ParcelaResponse> updateParcela(
            @PathVariable Long etapaId,
            @PathVariable Long parcelaId,
            @Valid @RequestBody ParcelaRequest request) {
        return ResponseEntity.ok(parcelaService.updateParcela(parcelaId, request));
    }
    
    @DeleteMapping("/api/etapas/{etapaId}/parcelas/{parcelaId}")
    public ResponseEntity<Void> deleteParcela(@PathVariable Long etapaId, @PathVariable Long parcelaId) {
        parcelaService.deleteParcela(parcelaId);
        return ResponseEntity.noContent().build();
    }
}
