package com.loadtrack.controller;

import com.loadtrack.dto.ApiResponse;
import com.loadtrack.dto.TruckRequest;
import com.loadtrack.entity.Truck;
import com.loadtrack.service.TruckService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trucks")
public class TruckController {

    @Autowired
    private TruckService truckService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Truck>>> getAllTrucks() {
        return ResponseEntity.ok(ApiResponse.success(truckService.getAllTrucks()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Truck>> getTruckById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(truckService.getTruckById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Truck>> addTruck(@Valid @RequestBody TruckRequest request) {
        Truck truck = truckService.addTruck(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Truck added successfully", truck));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Truck>> updateTruck(
            @PathVariable Integer id,
            @Valid @RequestBody TruckRequest request) {
        Truck truck = truckService.updateTruck(id, request);
        return ResponseEntity.ok(ApiResponse.success("Truck updated successfully", truck));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTruck(@PathVariable Integer id) {
        truckService.deleteTruck(id);
        return ResponseEntity.ok(ApiResponse.success("Truck deleted successfully", null));
    }
}
