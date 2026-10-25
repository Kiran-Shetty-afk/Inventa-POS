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
public class StoreOverviewDTO {
    private Integer totalBranches;
    private BigDecimal totalSales;
    private BigDecimal previousPeriodSales;
    private Integer totalOrders;
    private Integer totalEmployees;
    private Integer totalCustomers;
    private Integer totalRefunds;
    private Integer totalProducts;
    private Integer todayOrders;
    private Integer yesterdayOrders;
    private Integer activeCashiers;
    private BigDecimal averageOrderValue;
    private BigDecimal previousPeriodAverageOrderValue;
    private String topBranchName;
}
