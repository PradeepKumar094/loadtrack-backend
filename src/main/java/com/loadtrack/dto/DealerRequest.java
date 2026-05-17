package com.loadtrack.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DealerRequest {

    @NotBlank(message = "Dealer name is required")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phone;

    private String address;

    // Login credentials — auto-create account
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
