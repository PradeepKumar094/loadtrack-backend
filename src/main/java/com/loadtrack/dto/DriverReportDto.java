package com.loadtrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverReportDto {

    private Integer driverId;
    private String driverName;
    private String phone;
    private String licenseNumber;
    private BigDecimal salaryPerTrip;
    private String assignedTruck;

    // Trip summary
    private long totalTrips;
    private long completedTrips;
    private long pendingTrips;
    private long cancelledTrips;
    private BigDecimal totalTonsCarried;

    // Salary summary
    private BigDecimal totalSalaryEarned;   // completedTrips * salaryPerTrip
    private BigDecimal salaryCredited;      // what has been paid to driver (future feature, 0 for now)
    private BigDecimal salaryPending;       // totalSalaryEarned - salaryCredited

    // Trip details
    private List<TripSummary> trips;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripSummary {
        private Integer tripId;
        private String tripDate;
        private String truckNumber;
        private String dealerName;
        private String sandType;
        private BigDecimal tons;
        private String source;
        private String destination;
        private BigDecimal totalAmount;
        private String status;
        private BigDecimal salaryForTrip;
    }
}
