package com.zosh.analytics.controller;

import com.zosh.analytics.dto.AnalyticsDashboardDTO;
import com.zosh.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public AnalyticsDashboardDTO getDashboard() {

        return AnalyticsDashboardDTO.builder()
                .totalOrders(100L)
                .totalRevenue(50000D)
                .averageOrderValue(500D)
                .highestOrderValue(2000D)
                .build();
    }

}