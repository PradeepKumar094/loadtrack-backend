package com.loadtrack.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TruckRequest {

    @NotBlank(message = "Truck number is required")
    private String truckNumber;

    @NotBlank(message = "Model is required")
    private String model;

    @NotNull(message = "Capacity is required")
    @DecimalMin(value = "0.1", message = "Capacity must be greater than 0")
    private BigDecimal capacityTons;

    @NotBlank(message = "Insurance number is required")
    private String insuranceNumber;

    @NotBlank(message = "RC number is required")
    private String rcNumber;

    private String status;
}
