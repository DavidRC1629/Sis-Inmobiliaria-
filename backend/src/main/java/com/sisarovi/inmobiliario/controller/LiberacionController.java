package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.dto.LiberacionLoteResponse;
import com.sisarovi.inmobiliario.dto.LiberacionRequest;
import com.sisarovi.inmobiliario.service.LiberacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/liberaciones")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LiberacionController {

    private final LiberacionService liberacionService;

    @GetMapping("/lotes")
    public ResponseEntity<List<LiberacionLoteResponse>> listarLotesAdquiridos(
            @RequestParam Long projectId
    ) {
        return ResponseEntity.ok(liberacionService.listarLotesAdquiridosPorProyecto(projectId));
    }

    @PostMapping("/lotes/{loteId}")
    public ResponseEntity<Void> liberarLote(
            @PathVariable Long loteId,
            @Valid @RequestBody LiberacionRequest request,
            Authentication authentication
    ) {
        liberacionService.liberarLote(
                loteId,
                request.getDescripcion(),
                request.getAdminPassword(),
                authentication != null ? authentication.getName() : "ANONIMO"
        );
        return ResponseEntity.noContent().build();
    }
}