package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.dto.AuthResponse;
import com.sisarovi.inmobiliario.dto.LoginRequest;
import com.sisarovi.inmobiliario.dto.RegisterRequest;
import com.sisarovi.inmobiliario.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password/request")
    public ResponseEntity<Map<String, String>> requestPasswordRecovery(@RequestBody Map<String, String> payload) {
        String email = payload != null ? payload.get("email") : null;
        return ResponseEntity.ok(authService.requestPasswordRecovery(email));
    }

    @PostMapping("/forgot-password/confirm")
    public ResponseEntity<Map<String, String>> confirmPasswordRecovery(@RequestBody Map<String, String> payload) {
        String email = payload != null ? payload.get("email") : null;
        String temporaryCode = payload != null ? payload.get("temporaryCode") : null;
        String newPassword = payload != null ? payload.get("newPassword") : null;
        return ResponseEntity.ok(authService.confirmPasswordRecovery(email, temporaryCode, newPassword));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.getCurrentUserProfile(authentication.getName()));
    }
}
