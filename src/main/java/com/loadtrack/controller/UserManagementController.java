package com.loadtrack.controller;

import com.loadtrack.dto.*;
import com.loadtrack.entity.Dealer;
import com.loadtrack.entity.Driver;
import com.loadtrack.repository.UserRepository;
import com.loadtrack.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    @Autowired private UserManagementService userManagementService;
    @Autowired private UserRepository userRepository;

    // ── Check username availability ───────────────────────────────
    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Boolean>> checkUsername(@RequestParam String username) {
        boolean available = !userRepository.existsByUsername(username);
        String msg = available ? "Username is available" : "Username '" + username + "' is already taken. Try another.";
        return ResponseEntity.ok(new ApiResponse<>(available, msg, available));
    }

    // ── Get all users ─────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getAllUsers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getUserById(id)));
    }

    // ── Create login account (standalone) ────────────────────────
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        try {
            UserResponse user = userManagementService.createUser(request);
            return ResponseEntity.status(201)
                    .body(ApiResponse.success("User account created successfully", user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Toggle active/inactive ────────────────────────────────────
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleStatus(@PathVariable Integer id) {
        try {
            UserResponse user = userManagementService.toggleStatus(id);
            return ResponseEntity.ok(ApiResponse.success(
                    user.getStatus() ? "Account activated" : "Account deactivated", user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Reset password ────────────────────────────────────────────
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Integer id,
            @Valid @RequestBody ResetPasswordRequest request) {
        try {
            userManagementService.resetPassword(id, request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Delete user account ───────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Integer id) {
        try {
            userManagementService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.success("User account deleted", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Drivers/Dealers without accounts ─────────────────────────
    @GetMapping("/available-drivers")
    public ResponseEntity<ApiResponse<List<Driver>>> getDriversWithoutAccount() {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getDriversWithoutAccount()));
    }

    @GetMapping("/available-dealers")
    public ResponseEntity<ApiResponse<List<Dealer>>> getDealersWithoutAccount() {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getDealersWithoutAccount()));
    }
}
