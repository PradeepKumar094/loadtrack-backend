package com.loadtrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String role;
    private Integer userId;
    private Integer linkedId;   // driver ID or dealer ID if applicable
}
