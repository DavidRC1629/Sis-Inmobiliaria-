package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.dto.LoteRequest;
import com.sisarovi.inmobiliario.dto.LoteResponse;
import com.sisarovi.inmobiliario.service.LoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parcelas/{parcelaId}/lotes")
@RequiredArgsConstructor
public class LoteController {
    
    private final LoteService loteService;
    
    @GetMapping
    public ResponseEntity<List<LoteResponse>> getLotesByParcela(@PathVariable Long parcelaId) {
        return ResponseEntity.ok(loteService.getLotesByParcela(parcelaId));
    }
    
    @GetMapping("/manzana/{manzana}")
    public ResponseEntity<List<LoteResponse>> getLotesByManzana(
            @PathVariable Long parcelaId,
            @PathVariable String manzana) {
        return ResponseEntity.ok(loteService.getLotesByParcelaAndManzana(parcelaId, manzana));
    }
    
    @GetMapping("/{loteId}")
    public ResponseEntity<LoteResponse> getLoteById(@PathVariable Long parcelaId, @PathVariable Long loteId) {
        return ResponseEntity.ok(loteService.getLoteById(loteId));
    }

    @GetMapping("/numero-partida/existe")
    public ResponseEntity<Boolean> existsNumeroPartida(
            @PathVariable Long parcelaId,
            @RequestParam String numeroPartida,
            @RequestParam(required = false) Long excludeLoteId) {
        return ResponseEntity.ok(loteService.existsNumeroPartidaGlobal(numeroPartida, excludeLoteId));
    }
    
    @PostMapping
    public ResponseEntity<LoteResponse> createLote(
            @PathVariable Long parcelaId,
            @Valid @RequestBody LoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loteService.createLote(parcelaId, request));
    }
    
    @PutMapping("/{loteId}")
    public ResponseEntity<LoteResponse> updateLote(
            @PathVariable Long parcelaId,
            @PathVariable Long loteId,
            @Valid @RequestBody LoteRequest request) {
        return ResponseEntity.ok(loteService.updateLote(loteId, request));
    }
    
    @DeleteMapping("/{loteId}")
    public ResponseEntity<Void> deleteLote(@PathVariable Long parcelaId, @PathVariable Long loteId) {
        loteService.deleteLote(loteId);
        return ResponseEntity.noContent().build();
    }
}
