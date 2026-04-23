package com.zosh.payload.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BranchHealthCopilotResponseDTO {
    private String headline;
    private String summary;
    private List<String> highlights;
    private List<String> risks;
    private List<String> recommendedActions;
    private BranchHealthSupportingMetricsDTO supportingMetrics;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    public static class BranchHealthSupportingMetricsDTO {
        private String mode;
        private LocalDateTime windowStart;
        private LocalDateTime windowEnd;
        private BigDecimal totalSales;
        private double salesGrowth;
        private int orderCount;
        private int activeCashiers;
        private String topCashierName;
        private double topCashierRevenue;
        private String topCategoryName;
        private double topCategorySales;
        private int lowStockItems;
        private int refundCount;
        private double refundAmount;
        private Integer refundSpikeHour;
        private int refundSpikeCount;
    }
}
