package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.dto.TerrenoPropioRequest;
import com.sisarovi.inmobiliario.dto.TerrenoPropioResponse;
import com.sisarovi.inmobiliario.dto.AdquisicionTerrenoRequest;
import com.sisarovi.inmobiliario.service.TerrenoPropioService;
import com.sisarovi.inmobiliario.service.RegistroAuditoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/terrenos-propios")
@RequiredArgsConstructor
@Slf4j
public class TerrenoPropioController {
    private final TerrenoPropioService terrenoPropioService;
    private final RegistroAuditoriaService registroAuditoriaService;

    @GetMapping
    public ResponseEntity<List<TerrenoPropioResponse>> getAll() {
        return ResponseEntity.ok(terrenoPropioService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TerrenoPropioResponse> getById(@PathVariable Long id) {
        return terrenoPropioService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TerrenoPropioResponse> create(@Valid @RequestBody TerrenoPropioRequest request) {
        TerrenoPropioResponse creado = terrenoPropioService.create(request);
        registroAuditoriaService.registrarAccion(
            String.valueOf(request.getPropietarioId()),
            "CREATE",
            String.format("Se creó el terreno propio con partida '%s'", request.getNumeroPartida())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/exists/partida/{numeroPartida}")
    public ResponseEntity<Boolean> existsByNumeroPartida(@PathVariable String numeroPartida) {
        return ResponseEntity.ok(terrenoPropioService.existsByNumeroPartida(numeroPartida));
    }

    // Subida de imagen
    @PostMapping("/{id}/imagen")
    public ResponseEntity<String> uploadImagen(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String url = terrenoPropioService.uploadImagen(id, file);
        return ResponseEntity.ok(url);
    }

    // Eliminar imagen
    @DeleteMapping("/{id}/imagen")
    public ResponseEntity<Void> deleteImagen(@PathVariable Long id) {
        terrenoPropioService.deleteImagen(id);
        return ResponseEntity.noContent().build();
    }

    // Adquirir terreno
    @PostMapping("/{id}/adquirir")
    public ResponseEntity<Void> adquirirTerreno(@PathVariable Long id, @RequestBody AdquisicionTerrenoRequest request) {
        terrenoPropioService.adquirirTerreno(id, request.getClienteId(), request.getFormaPago(),
                request.getCuotas() != null ? request.getCuotas() : 1,
                request.getInteres() != null ? request.getInteres() : 0.0);
        return ResponseEntity.ok().build();
    }
}
