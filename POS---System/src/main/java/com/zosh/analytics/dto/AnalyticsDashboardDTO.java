package com.zosh.analytics.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsDashboardDTO {

    private Long totalOrders;

    private Double totalRevenue;

    private Double averageOrderValue;

    private Double highestOrderValue;

}