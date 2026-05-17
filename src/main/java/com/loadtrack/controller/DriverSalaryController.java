package com.loadtrack.controller;

import com.loadtrack.dto.ApiResponse;
import com.loadtrack.entity.Driver;
import com.loadtrack.entity.DriverSalaryCredit;
import com.loadtrack.entity.Trip;
import com.loadtrack.exception.ResourceNotFoundException;
import com.loadtrack.repository.DriverRepository;
import com.loadtrack.repository.DriverSalaryCreditRepository;
import com.loadtrack.repository.TripRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/driver-salary")
public class DriverSalaryController {

    @Autowired private DriverSalaryCreditRepository creditRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private TripRepository tripRepository;

    // Get salary summary for a driver
    @GetMapping("/{driverId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN','DRIVER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalarySummary(@PathVariable Integer driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        List<Trip> trips = tripRepository.findByDriverId(driverId);

        BigDecimal totalEarned = trips.stream()
                .filter(t -> t.getStatus() == Trip.TripStatus.COMPLETED)
                .map(Trip::getDriverSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredited = creditRepository.getTotalCreditedByDriver(driverId);
        BigDecimal pending = totalEarned.subtract(totalCredited);

        List<DriverSalaryCredit> credits = creditRepository.findByDriverIdOrderByCreditedAtDesc(driverId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("driverId", driverId);                          // ← added
        summary.put("driverName", driver.getName());
        summary.put("phone", driver.getPhone());
        summary.put("salaryPerTrip", driver.getSalaryPerTrip());
        summary.put("totalEarned", totalEarned);
        summary.put("totalCredited", totalCredited);
        summary.put("pendingWithOwner", pending);
        summary.put("creditHistory", credits);

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // Admin credits salary to driver
    @PostMapping("/{driverId}/credit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DriverSalaryCredit>> creditSalary(
            @PathVariable Integer driverId,
            @RequestBody CreditRequest request) {

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        DriverSalaryCredit credit = new DriverSalaryCredit();
        credit.setDriver(driver);
        credit.setAmount(request.getAmount());
        credit.setRemarks(request.getRemarks() != null ? request.getRemarks() : "Salary credited by admin");

        if (request.getTripId() != null) {
            tripRepository.findById(request.getTripId()).ifPresent(credit::setTrip);
        }

        DriverSalaryCredit saved = creditRepository.save(credit);
        return ResponseEntity.ok(ApiResponse.success("Salary credited successfully", saved));
    }

    @Data
    static class CreditRequest {
        @NotNull @DecimalMin("0.01")
        private BigDecimal amount;
        private String remarks;
        private Integer tripId;  // optional — null if not linked to specific trip
    }
}
