package com.loadtrack.controller;

import com.loadtrack.dto.ApiResponse;
import com.loadtrack.dto.DashboardResponse;
import com.loadtrack.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getAdminDashboard() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getAdminDashboard()));
    }

    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN','DRIVER')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDriverDashboard(
            @PathVariable Integer driverId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDriverDashboard(driverId)));
    }

    @GetMapping("/dealer/{dealerId}")
    @PreAuthorize("hasAnyRole('ADMIN','DEALER')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDealerDashboard(
            @PathVariable Integer dealerId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDealerDashboard(dealerId)));
    }
}
