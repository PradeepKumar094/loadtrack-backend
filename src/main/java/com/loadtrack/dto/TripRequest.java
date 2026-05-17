package com.loadtrack.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TripRequest {

    @NotNull(message = "Truck is required")
    private Integer truckId;

    @NotNull(message = "Driver is required")
    private Integer driverId;

    @NotNull(message = "Dealer is required")
    private Integer dealerId;

    @NotNull(message = "Sand type is required")
    private Integer sandTypeId;

    @NotNull(message = "Tons is required")
    @DecimalMin(value = "0.1", message = "Tons must be greater than 0")
    private BigDecimal tons;

    @NotBlank(message = "Source location is required")
    private String sourceLocation;

    @NotBlank(message = "Destination location is required")
    private String destinationLocation;

    @NotNull(message = "Distance is required")
    @DecimalMin(value = "0.0", message = "Distance cannot be negative")
    private BigDecimal distanceKm = BigDecimal.ZERO;

    @NotNull(message = "Trip date is required")
    private LocalDate tripDate;

    private String status;

    // Optional initial payment at trip creation
    private BigDecimal initialPayment;
    private String paymentRemarks;
}
