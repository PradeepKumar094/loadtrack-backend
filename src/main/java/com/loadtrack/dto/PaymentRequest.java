package com.loadtrack.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentRequest {

    @NotNull(message = "Paid amount is required")
    @DecimalMin(value = "0.01", message = "Paid amount must be greater than 0")
    private BigDecimal paidAmount;

    private LocalDate paymentDate;

    private String remarks;  // Optional note for this payment entry
}
