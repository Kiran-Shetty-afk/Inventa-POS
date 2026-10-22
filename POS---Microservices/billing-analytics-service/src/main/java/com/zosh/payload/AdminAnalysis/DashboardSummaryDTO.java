package com.zosh.payload.AdminAnalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {
    private Long totalStores;
    private Long activeStores;
    private Long pendingStores;
    private Double totalPlatformRevenue;
}
