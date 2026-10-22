package com.zosh.payload.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BranchHealthCopilotRequestDTO {
    private Long branchId;
    private LocalDate date;
    private Integer year;
    private Integer month;
    private Integer days;
}
