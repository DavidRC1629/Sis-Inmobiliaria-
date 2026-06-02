package com.sisarovi.inmobiliario.controller;

import com.sisarovi.inmobiliario.dto.DeleteUserRequest;
import com.sisarovi.inmobiliario.entity.User;
import com.sisarovi.inmobiliario.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @GetMapping("/users/waiting")
    public ResponseEntity<List<User>> getRejectedWaitingUsers() {
        return ResponseEntity.ok(adminService.getRejectedWaitingUsers());
    }

    @PostMapping("/users/{userId}/approve")
    public ResponseEntity<User> approveUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.approveUser(userId));
    }

    @PostMapping("/users/{userId}/reject")
    public ResponseEntity<User> rejectUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.rejectUser(userId));
    }

    @PostMapping("/users/{userId}/cancel-wait")
    public ResponseEntity<User> cancelRejectionCooldown(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.cancelRejectionCooldown(userId));
    }

    @PostMapping("/users/{userId}/promote")
    public ResponseEntity<User> promoteToAdmin(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.promoteToAdmin(userId));
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @DeleteMapping("/users/{userId}/with-password")
    public ResponseEntity<Void> deleteUserWithPassword(
            @PathVariable Long userId,
            @RequestBody DeleteUserRequest request,
            Authentication authentication
    ) {
        adminService.deleteUserWithPassword(userId, authentication.getName(), request.getPassword());
        return ResponseEntity.noContent().build();
    }
}