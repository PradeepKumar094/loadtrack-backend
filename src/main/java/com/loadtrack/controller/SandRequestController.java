package com.loadtrack.controller;

import com.loadtrack.dto.*;
import com.loadtrack.entity.SandRequest;
import com.loadtrack.service.SandRequestService;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sand-requests")
public class SandRequestController {

    @Autowired
    private SandRequestService sandRequestService;

    // ── Dealer endpoints ──────────────────────────────────────────

    // Dealer submits a sand request
    @PostMapping("/dealer/{dealerId}")
    @PreAuthorize("hasAnyRole('ADMIN','DEALER')")
    public ResponseEntity<ApiResponse<SandRequest>> createRequest(
            @PathVariable Integer dealerId,
            @Valid @RequestBody SandRequestDto dto) {
        try {
            SandRequest req = sandRequestService.createRequest(dealerId, dto);
            return ResponseEntity.status(201)
                    .body(ApiResponse.success("Sand request submitted successfully", req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // Dealer views their own requests
    @GetMapping("/dealer/{dealerId}")
    @PreAuthorize("hasAnyRole('ADMIN','DEALER')")
    public ResponseEntity<ApiResponse<List<SandRequest>>> getByDealer(@PathVariable Integer dealerId) {
        return ResponseEntity.ok(ApiResponse.success(sandRequestService.getRequestsByDealer(dealerId)));
    }

    // ── Admin endpoints ───────────────────────────────────────────

    // Admin views all requests
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SandRequest>>> getAllRequests() {
        return ResponseEntity.ok(ApiResponse.success(sandRequestService.getAllRequests()));
    }

    // Admin views pending requests only
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SandRequest>>> getPendingRequests() {
        return ResponseEntity.ok(ApiResponse.success(sandRequestService.getPendingRequests()));
    }

    // Admin accepts request → auto-creates trip
    @PutMapping("/{id}/accept")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SandRequest>> acceptRequest(
            @PathVariable Integer id,
            @Valid @RequestBody AcceptRequestDto dto) {
        try {
            SandRequest req = sandRequestService.acceptRequest(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Request accepted and trip created", req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // Admin rejects request
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SandRequest>> rejectRequest(
            @PathVariable Integer id,
            @RequestBody RejectRequest body) {
        try {
            SandRequest req = sandRequestService.rejectRequest(id, body.getAdminRemarks());
            return ResponseEntity.ok(ApiResponse.success("Request rejected", req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Data
    static class RejectRequest {
        private String adminRemarks;
    }
}
