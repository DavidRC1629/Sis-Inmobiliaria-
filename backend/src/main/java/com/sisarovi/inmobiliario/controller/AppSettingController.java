package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class AppSettingController {

    private final AppSettingService appSettingService;

    @GetMapping("/logo-arovi")
    public ResponseEntity<Map<String, String>> getLogoArovi() {
        return ResponseEntity.ok(Map.of("logoAroviUrl", appSettingService.getLogoAroviUrl()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/logo-arovi")
    public ResponseEntity<Map<String, String>> updateLogoArovi(@RequestBody Map<String, String> request) {
        String value = request != null ? request.getOrDefault("logoAroviUrl", "") : "";
        String saved = appSettingService.saveLogoAroviUrl(value);
        return ResponseEntity.ok(Map.of("logoAroviUrl", saved));
    }
}
