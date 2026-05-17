package com.loadtrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "settings")
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "interest_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRatePercent = new BigDecimal("2.00");

    @Column(name = "allowed_days", nullable = false)
    private Integer allowedDays = 30;

    // Distance charge settings
    @Column(name = "base_distance_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal baseDistanceKm = new BigDecimal("12.00");

    @Column(name = "extra_charge_per_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal extraChargePerKm = new BigDecimal("30.00");

    @Column(name = "driver_extra_share_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal driverExtraSharePct = new BigDecimal("50.00");

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
