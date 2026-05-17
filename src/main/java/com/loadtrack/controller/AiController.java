package com.loadtrack.controller;

import com.loadtrack.dto.ApiResponse;
import com.loadtrack.service.GeminiService;
import com.loadtrack.service.PaymentRiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@PreAuthorize("hasRole('ADMIN')")
public class AiController {

    @Autowired private GeminiService geminiService;
    @Autowired private PaymentRiskService paymentRiskService;

    // ── Chatbot ───────────────────────────────────────────────────
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<String>> chat(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Question is required"));
        }
        String answer = geminiService.chat(question);
        return ResponseEntity.ok(ApiResponse.success(answer));
    }

    // ── Payment Risk ──────────────────────────────────────────────
    @GetMapping("/payment-risk")
    public ResponseEntity<ApiResponse<List<PaymentRiskService.DealerRisk>>> getAllRisks() {
        return ResponseEntity.ok(ApiResponse.success(paymentRiskService.getAllDealerRisks()));
    }

    @GetMapping("/payment-risk/{dealerId}")
    public ResponseEntity<ApiResponse<PaymentRiskService.DealerRisk>> getDealerRisk(
            @PathVariable Integer dealerId) {
        return ResponseEntity.ok(ApiResponse.success(paymentRiskService.getDealerRisk(dealerId)));
    }
}
