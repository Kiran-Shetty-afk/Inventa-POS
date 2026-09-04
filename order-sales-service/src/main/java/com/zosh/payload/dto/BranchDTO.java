package com.zosh.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Long storeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String manager;

    public BranchDTO(Long id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }
}
