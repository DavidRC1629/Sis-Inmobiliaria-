package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.dto.DevolucionPagoRequest;
import com.sisarovi.inmobiliario.dto.DevolucionRequest;
import com.sisarovi.inmobiliario.dto.DevolucionResponse;
import com.sisarovi.inmobiliario.service.DevolucionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/devoluciones")
@RequiredArgsConstructor
public class DevolucionController {
    private final DevolucionService devolucionService;

    @GetMapping
    public ResponseEntity<List<DevolucionResponse>> listar(@RequestParam(required = false) String estado) {
        return ResponseEntity.ok(devolucionService.listar(estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DevolucionResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(devolucionService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<DevolucionResponse> crear(
            @Valid @RequestBody DevolucionRequest request,
            Authentication authentication) {
        String usuario = authentication != null ? String.valueOf(authentication.getName()) : "ANONIMO";
        return ResponseEntity.ok(devolucionService.crear(request, usuario));
    }

    @PostMapping("/{id}/pagos")
    public ResponseEntity<DevolucionResponse> registrarPago(
            @PathVariable Long id,
            @Valid @RequestBody DevolucionPagoRequest request,
            Authentication authentication) {
        String usuario = authentication != null ? String.valueOf(authentication.getName()) : "ANONIMO";
        return ResponseEntity.ok(devolucionService.registrarPago(id, request, usuario));
    }
}