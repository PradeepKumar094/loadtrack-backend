package com.loadtrack.controller;

import com.loadtrack.dto.ApiResponse;
import com.loadtrack.dto.DealerReportDto;
import com.loadtrack.dto.DriverReportDto;
import com.loadtrack.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    // ── Trips Excel ───────────────────────────────────────────────

    @GetMapping("/trips/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportTripsExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to)
            throws IOException {
        byte[] data = reportService.exportTripsToExcel(from, to);
        return excelResponse(data, "trips_report.xlsx");
    }

    @GetMapping("/payments/pending/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportPendingPaymentsExcel() throws IOException {
        byte[] data = reportService.exportPendingPaymentsToExcel();
        return excelResponse(data, "pending_payments.xlsx");
    }

    // ── Driver Reports ────────────────────────────────────────────

    @GetMapping("/drivers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DriverReportDto>>> getAllDriverReports() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getAllDriverReports()));
    }

    @GetMapping("/drivers/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DriverReportDto>> getDriverReport(@PathVariable Integer driverId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getDriverReport(driverId)));
    }

    @GetMapping("/drivers/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportAllDriversExcel() throws IOException {
        byte[] data = reportService.exportDriverReportToExcel(null);
        return excelResponse(data, "drivers_report.xlsx");
    }

    @GetMapping("/drivers/{driverId}/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportDriverExcel(@PathVariable Integer driverId) throws IOException {
        byte[] data = reportService.exportDriverReportToExcel(driverId);
        return excelResponse(data, "driver_" + driverId + "_report.xlsx");
    }

    // ── Dealer Reports ────────────────────────────────────────────

    @GetMapping("/dealers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DealerReportDto>>> getAllDealerReports() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getAllDealerReports()));
    }

    @GetMapping("/dealers/{dealerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DealerReportDto>> getDealerReport(@PathVariable Integer dealerId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getDealerReport(dealerId)));
    }

    @GetMapping("/dealers/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportAllDealersExcel() throws IOException {
        byte[] data = reportService.exportDealerReportToExcel(null);
        return excelResponse(data, "dealers_report.xlsx");
    }

    @GetMapping("/dealers/{dealerId}/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportDealerExcel(@PathVariable Integer dealerId) throws IOException {
        byte[] data = reportService.exportDealerReportToExcel(dealerId);
        return excelResponse(data, "dealer_" + dealerId + "_report.xlsx");
    }

    // ── Helper ────────────────────────────────────────────────────

    private ResponseEntity<byte[]> excelResponse(byte[] data, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
