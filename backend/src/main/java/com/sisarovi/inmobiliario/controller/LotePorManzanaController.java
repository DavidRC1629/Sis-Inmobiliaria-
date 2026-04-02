package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.entity.Lote;
import com.sisarovi.inmobiliario.repository.LoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manzanas/{manzanaId}/lotes")
@RequiredArgsConstructor
public class LotePorManzanaController {
    private final LoteRepository loteRepository;

    @GetMapping
    public ResponseEntity<List<Lote>> getLotesByManzana(@PathVariable Long manzanaId) {
        return ResponseEntity.ok(loteRepository.findByManzanaIdOrderByNumeroAsc(manzanaId));
    }
}
