package com.zosh.payload.StoreAnalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchPerformanceDTO {
    private String branchName;
    private BigDecimal totalSales;
    private Integer totalOrders;
}
