package com.zosh.payload.StoreAnalysis;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StoreOverviewDTO {
    private Integer totalBranches;
    private Double totalSales;
    private Double previousPeriodSales;
    private Integer totalOrders;
    private Integer totalEmployees;
    private Integer totalCustomers;
    private Integer totalRefunds;
    private Integer totalProducts;
    private Integer todayOrders;
    private Integer yesterdayOrders;
    private Integer activeCashiers;
    private Double averageOrderValue;
    private Double previousPeriodAverageOrderValue;
    private String topBranchName;
}


