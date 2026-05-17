package com.loadtrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "trucks")
public class Truck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "truck_number", nullable = false, unique = true, length = 50)
    private String truckNumber;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "capacity_tons", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacityTons;

    @Column(name = "insurance_number", nullable = false, length = 100)
    private String insuranceNumber;

    @Column(name = "rc_number", nullable = false, length = 100)
    private String rcNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TruckStatus status = TruckStatus.AVAILABLE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum TruckStatus {
        AVAILABLE, ON_TRIP, MAINTENANCE
    }
}
