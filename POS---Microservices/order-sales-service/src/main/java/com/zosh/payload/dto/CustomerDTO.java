package com.zosh.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Long storeId;
    private Long branchId;
    private Integer loyaltyPoints;
    private String loyaltyStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
