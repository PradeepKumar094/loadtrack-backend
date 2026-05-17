package com.loadtrack.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DriverRequest {

    @NotBlank(message = "Driver name is required")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phone;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    private String address;

    @NotNull(message = "Salary per trip is required")
    @DecimalMin(value = "0.0", message = "Salary cannot be negative")
    private BigDecimal salaryPerTrip;

    private Integer assignedTruckId;

    // Login credentials — auto-create account
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
