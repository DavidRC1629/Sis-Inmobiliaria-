package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users/pending")
    public ResponseEntity<List<User>> getPendingUsers() {
        return ResponseEntity.ok(adminService.getPendingUsers());
    }

    @PostMapping("/users/{userId}/approve")
    public ResponseEntity<User> approveUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(adminService.approveUser(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/users/{userId}/reject")
    public ResponseEntity<User> rejectUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(adminService.rejectUser(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }
}