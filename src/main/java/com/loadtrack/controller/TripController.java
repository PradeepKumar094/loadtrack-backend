package com.loadtrack.controller;

import com.loadtrack.dto.ApiResponse;
import com.loadtrack.dto.TripRequest;
import com.loadtrack.entity.Trip;
import com.loadtrack.service.TripService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    @Autowired
    private TripService tripService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Trip>>> getAllTrips() {
        return ResponseEntity.ok(ApiResponse.success(tripService.getAllTrips()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DRIVER','DEALER')")
    public ResponseEntity<ApiResponse<Trip>> getTripById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(tripService.getTripById(id)));
    }

    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN','DRIVER')")
    public ResponseEntity<ApiResponse<List<Trip>>> getTripsByDriver(@PathVariable Integer driverId) {
        return ResponseEntity.ok(ApiResponse.success(tripService.getTripsByDriver(driverId)));
    }

    @GetMapping("/dealer/{dealerId}")
    @PreAuthorize("hasAnyRole('ADMIN','DEALER')")
    public ResponseEntity<ApiResponse<List<Trip>>> getTripsByDealer(@PathVariable Integer dealerId) {
        return ResponseEntity.ok(ApiResponse.success(tripService.getTripsByDealer(dealerId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Trip>> createTrip(@Valid @RequestBody TripRequest request) {
        Trip trip = tripService.createTrip(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Trip created successfully", trip));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Trip>> updateTrip(
            @PathVariable Integer id,
            @Valid @RequestBody TripRequest request) {
        Trip trip = tripService.updateTrip(id, request);
        return ResponseEntity.ok(ApiResponse.success("Trip updated successfully", trip));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(@PathVariable Integer id) {
        tripService.deleteTrip(id);
        return ResponseEntity.ok(ApiResponse.success("Trip deleted successfully", null));
    }

    // Driver acknowledges trip
    @PutMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN','DRIVER')")
    public ResponseEntity<ApiResponse<Trip>> acknowledgeTrip(
            @PathVariable Integer id,
            @RequestParam Integer driverId) {
        try {
            Trip trip = tripService.acknowledgeTrip(id, driverId);
            return ResponseEntity.ok(ApiResponse.success("Trip acknowledged successfully", trip));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // Driver marks trip as completed
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','DRIVER')")
    public ResponseEntity<ApiResponse<Trip>> driverCompleteTrip(
            @PathVariable Integer id,
            @RequestParam Integer driverId) {
        try {
            Trip trip = tripService.driverCompleteTrip(id, driverId);
            return ResponseEntity.ok(ApiResponse.success("Trip marked as completed", trip));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
