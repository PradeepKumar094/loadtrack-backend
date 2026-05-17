package com.loadtrack.controller;

import com.loadtrack.dto.ApiResponse;
import com.loadtrack.entity.Receipt;
import com.loadtrack.service.ReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    @Autowired
    private ReceiptService receiptService;

    @GetMapping("/payment/{paymentId}")
    @PreAuthorize("hasAnyRole('ADMIN','DEALER')")
    public ResponseEntity<ApiResponse<List<Receipt>>> getReceiptsByPayment(
            @PathVariable Integer paymentId) {
        return ResponseEntity.ok(ApiResponse.success(receiptService.getReceiptsByPayment(paymentId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEALER')")
    public ResponseEntity<ApiResponse<Receipt>> getReceiptById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(receiptService.getReceiptById(id)));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','DEALER')")
    public ResponseEntity<byte[]> downloadReceiptPdf(@PathVariable Integer id) throws Exception {
        byte[] pdf = receiptService.generateReceiptPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
