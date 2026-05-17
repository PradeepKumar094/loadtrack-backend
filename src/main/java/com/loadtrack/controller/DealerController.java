package com.loadtrack.controller;

import com.loadtrack.dto.ApiResponse;
import com.loadtrack.dto.DealerRequest;
import com.loadtrack.entity.Dealer;
import com.loadtrack.service.DealerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dealers")
public class DealerController {

    @Autowired
    private DealerService dealerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEALER')")
    public ResponseEntity<ApiResponse<List<Dealer>>> getAllDealers() {
        return ResponseEntity.ok(ApiResponse.success(dealerService.getAllDealers()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEALER')")
    public ResponseEntity<ApiResponse<Dealer>> getDealerById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(dealerService.getDealerById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Dealer>> addDealer(@Valid @RequestBody DealerRequest request) {
        Dealer dealer = dealerService.addDealer(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Dealer added successfully", dealer));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Dealer>> updateDealer(
            @PathVariable Integer id,
            @Valid @RequestBody DealerRequest request) {
        Dealer dealer = dealerService.updateDealer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Dealer updated successfully", dealer));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDealer(@PathVariable Integer id) {
        dealerService.deleteDealer(id);
        return ResponseEntity.ok(ApiResponse.success("Dealer deleted successfully", null));
    }
}
