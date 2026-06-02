package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.service.DniApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reniec")
@RequiredArgsConstructor
public class ReniecController {
    private final DniApiService dniApiService;

    @GetMapping("/dni/{dni}")
    public ResponseEntity<?> getReniecDataByDni(@PathVariable String dni) {
        var result = dniApiService.consultarPorDniJson(dni);
        if (result == null) {
            var error = new java.util.HashMap<String, Object>();
            error.put("error", "No se encontraron datos para el DNI");
            return ResponseEntity.status(404).body(error);
        }
        // Convertir JSONObject a Map para respuesta limpia
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        result.toMap().forEach((k, v) -> response.put(k, v));
        System.out.println("✅ RENIEC Response for DNI " + dni + ": " + response);
        return ResponseEntity.ok(response);
    }
}
