package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.dto.AuthResponse;
import com.sisarovi.inmobiliario.dto.LoginRequest;
import com.sisarovi.inmobiliario.dto.RegisterRequest;
import com.sisarovi.inmobiliario.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
