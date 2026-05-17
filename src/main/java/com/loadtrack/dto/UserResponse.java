package com.loadtrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Integer id;
    private String username;
    private String role;
    private Boolean status;
    private String phone;

    // Linked entity info
    private Integer linkedDriverId;
    private String linkedDriverName;
    private Integer linkedDealerId;
    private String linkedDealerName;
}
