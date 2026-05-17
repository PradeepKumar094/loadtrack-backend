package com.loadtrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "truck_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Truck truck;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "assignedTruck"})
    private Driver driver;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dealer_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Dealer dealer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sand_type_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private SandType sandType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tons;

    @Column(name = "source_location", nullable = false, length = 255)
    private String sourceLocation;

    @Column(name = "destination_location", nullable = false, length = 255)
    private String destinationLocation;

    @Column(name = "distance_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal distanceKm = BigDecimal.ZERO;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @Column(name = "rate_per_ton", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerTon;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "extra_distance_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal extraDistanceCharge = BigDecimal.ZERO;

    @Column(name = "driver_extra_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal driverExtraAmount = BigDecimal.ZERO;

    @Column(name = "driver_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal driverSalary = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status = TripStatus.PENDING;

    @Column(name = "driver_acknowledged", nullable = false)
    private Boolean driverAcknowledged = false;

    @Column(name = "driver_completed", nullable = false)
    private Boolean driverCompleted = false;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (tons != null && ratePerTon != null) {
            totalAmount = tons.multiply(ratePerTon);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (tons != null && ratePerTon != null) {
            totalAmount = tons.multiply(ratePerTon);
        }
    }

    public enum TripStatus {
        PENDING, ACKNOWLEDGED, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
