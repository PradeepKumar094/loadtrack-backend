package com.loadtrack.controller;

import com.loadtrack.dto.ApiResponse;
import com.loadtrack.dto.PaymentRequest;
import com.loadtrack.entity.Payment;
import com.loadtrack.entity.PaymentTransaction;
import com.loadtrack.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Payment>>> getAllPayments() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getAllPayments()));
    }

    // ── IMPORTANT: specific paths BEFORE /{id} to avoid conflicts ──

    // Get payments pending admin verification
    @GetMapping("/pending-verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Payment>>> getPendingVerification() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPendingVerification()));
    }

    @GetMapping("/dealer/{dealerId}")
    @PreAuthorize("hasAnyRole('ADMIN','DEALER')")
    public ResponseEntity<ApiResponse<List<Payment>>> getPaymentsByDealer(@PathVariable Integer dealerId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentsByDealer(dealerId)));
    }

    @GetMapping("/trip/{tripId}")
    @PreAuthorize("hasAnyRole('ADMIN','DEALER')")
    public ResponseEntity<ApiResponse<Payment>> getPaymentByTrip(@PathVariable Integer tripId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentByTrip(tripId)));
    }

    // ── Generic /{id} paths AFTER specific paths ──

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEALER')")
    public ResponseEntity<ApiResponse<Payment>> getPaymentById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentById(id)));
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','DEALER')")
    public ResponseEntity<ApiResponse<List<PaymentTransaction>>> getTransactionLog(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getTransactionLog(id)));
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN','DEALER')")
    public ResponseEntity<ApiResponse<Payment>> makePayment(
            @PathVariable Integer id,
            @Valid @RequestBody PaymentRequest request) {
        Payment payment = paymentService.makePayment(id, request);
        return ResponseEntity.ok(ApiResponse.success("Payment submitted. Awaiting admin verification.", payment));
    }

    @PutMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Payment>> verifyPayment(@PathVariable Integer id) {
        try {
            Payment payment = paymentService.adminVerifyPayment(id);
            return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", payment));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Payment>> rejectPayment(
            @PathVariable Integer id,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : "Rejected by admin";
            Payment payment = paymentService.adminRejectPayment(id, reason);
            return ResponseEntity.ok(ApiResponse.success("Payment rejected", payment));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
