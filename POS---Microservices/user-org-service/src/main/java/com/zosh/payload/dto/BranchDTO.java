package com.zosh.payload.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDTO {
    private Long id;
    private String name;
    private String address;
    private String email;
    private String phone;
    private List<String> workingDays;
    private LocalTime openTime;
    private LocalTime closeTime;
    private BigDecimal maxDiscountPercent;
    private Long storeId;
    private StoreDTO store;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String manager;
    private Long managerId;
    private Boolean managerAssignmentProvided;

    public BranchDTO(Long id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }
}
