package com.loadtrack.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SandRequestDto {

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
    private BigDecimal distanceKm;

    @NotNull(message = "Requested date is required")
    private LocalDate requestedDate;

    private String remarks;
}
