package com.zosh.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchHealthSupportingMetricsDTO {
        private String mode;
        private LocalDateTime windowStart;
        private LocalDateTime windowEnd;
        private BigDecimal totalSales;
        private double salesGrowth;
        private int orderCount;
        private int activeCashiers;
        private String topCashierName;
        private BigDecimal topCashierRevenue;
        private String topCategoryName;
        private BigDecimal topCategorySales;
        private int lowStockItems;
        private int refundCount;
        private BigDecimal refundAmount;
        private Integer refundSpikeHour;
        private int refundSpikeCount;
    }
}
