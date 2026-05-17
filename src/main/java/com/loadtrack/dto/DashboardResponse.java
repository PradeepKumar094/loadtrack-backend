package com.loadtrack.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DashboardResponse {
    // Admin dashboard
    private long totalTrucks;
    private long totalDrivers;
    private long totalDealers;
    private long totalTrips;
    private long pendingPayments;
    private long pendingVerifications;
    private long pendingSandRequests;
    private BigDecimal monthlyEarnings;

    // Driver dashboard
    private long assignedTrips;
    private long completedTrips;
    private long pendingTrips;
    private long cancelledTrips;
    private BigDecimal totalSalaryEarned;

    // Dealer dashboard
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
}
