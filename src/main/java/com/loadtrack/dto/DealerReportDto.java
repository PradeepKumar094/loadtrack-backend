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
public class DealerReportDto {

    private Integer dealerId;
    private String dealerName;
    private String phone;
    private String address;

    // Trip summary
    private long totalTrips;
    private long completedTrips;
    private long pendingTrips;
    private BigDecimal totalTonsReceived;

    // Payment summary
    private BigDecimal totalBilled;       // sum of all trip amounts
    private BigDecimal totalPaid;         // sum of all paid amounts
    private BigDecimal totalPending;      // totalBilled - totalPaid
    private BigDecimal totalInterest;     // total interest applied

    // Trip + payment details
    private List<TripPaymentSummary> trips;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripPaymentSummary {
        private Integer tripId;
        private String tripDate;
        private String truckNumber;
        private String driverName;
        private String sandType;
        private BigDecimal tons;
        private BigDecimal tripAmount;
        private BigDecimal paidAmount;
        private BigDecimal pendingAmount;
        private BigDecimal interestAmount;
        private String paymentStatus;
        private String dueDate;
        private String tripStatus;
    }
}
