package com.loadtrack.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SandTypeRequest {

    @NotBlank(message = "Sand type name is required")
    private String name;

    @NotNull(message = "Price per ton is required")
    @DecimalMin(value = "0.1", message = "Price must be greater than 0")
    private BigDecimal pricePerTon;
}
