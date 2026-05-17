package com.loadtrack.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class AcceptRequestDto {

    @NotNull(message = "Truck is required")
    @Min(value = 1, message = "Please select a valid truck")
    private Integer truckId;

    @NotNull(message = "Driver is required")
    @Min(value = 1, message = "Please select a valid driver")
    private Integer driverId;

    private String adminRemarks;
}
