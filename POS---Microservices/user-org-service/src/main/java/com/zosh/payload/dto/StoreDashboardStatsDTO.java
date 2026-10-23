package com.zosh.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDashboardStatsDTO {
    private Long total;
    private Long active;
    private Long pending;
    private Long blocked;
    private Long closed;
}
